(ns metabase.driver.elasticsearch
  (:refer-clojure :exclude [second])
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure.set :as set]
            [metabase.models
             [field :as field :refer [Field]]]
            [metabase.driver :as driver]
            [metabase.driver.sql-jdbc
             [common :as sql-jdbc.common]
             [connection :as sql-jdbc.conn]
             [execute :as sql-jdbc.execute]
             [sync :as sql-jdbc.sync]]
            [metabase.util
             [honeysql-extensions :as hx]
             [i18n :refer [trs]]])
  (:import [java.sql Connection ResultSet Types DatabaseMetaData Timestamp]
           (java.time OffsetDateTime ZonedDateTime)))

(driver/register! :elasticsearch, :parent :sql-jdbc)

;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                          ES SQL Documentation                                                  |
;;; +----------------------------------------------------------------------------------------------------------------+
;;; https://www.elastic.co/guide/en/elasticsearch/reference/current/sql-spec.html

;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                          metabase.driver method impls                                          |
;;; +----------------------------------------------------------------------------------------------------------------+

(defmethod driver/supports? [:elasticsearch :foreign-keys] [_ _] false)

;(defmethod driver/supports? [:athena :nested-fields] [_ _] true)

;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                     metabase.driver.sql-jdbc method impls                                      |
;;; +----------------------------------------------------------------------------------------------------------------+

;;; ---------------------------------------------- sql-jdbc.connection -----------------------------------------------
;;; Reference:
;;; jdbc:[es|elasticsearch]://[[http|https]://]?[host[:port]]?/[prefix]?[\?[option=value]&]*

(defmethod sql-jdbc.conn/connection-details->spec :elasticsearch [_ {:keys [host port prefix user password], :as details}]
  (-> (merge
       {:classname        "org.elasticsearch.xpack.sql.jdbc.EsDriver"
        :subprotocol      "es"
        ; TODO: prefix -- good reference: https://github.com/metabase/metabase/blob/master/src/metabase/driver/postgres.clj#L302-L316
        :subname          (str "//http://" host ":" port "/")
        }
      (dissoc details :host :port :db :engine :prefix))
      (sql-jdbc.common/handle-additional-options details, :seperator-style :semicolon)))

;;; ------------------------------------------------- sql-jdbc.sync --------------------------------------------------

;; Map of column types -> Field base types
;; https://www.elastic.co/guide/en/elasticsearch/reference/current/sql-data-types.html
;; TODO: the below were copied from athena driver and should be updated to above match ^^^
;;       they need to be all tested
(defmethod sql-jdbc.sync/database-type->base-type :elasticsearch [_ database-type]
  ({
    :boolean              :type/Boolean
    :byte                 :type/Integer
    :short                :type/Integer
    :integer              :type/Integer
    :long                 :type/BigInteger
    :double               :type/Float
    :float                :type/Float
    :half_float           :type/Float
    :scaled_float         :type/Float
    :keyword              :type/Text
    :constant_keyword     :type/Text
    :text                 :type/Text
    :binary               :type/*
    :date                 :type/DateTime
    :ip                   :type/Text
    :object               :type/Dictionary
    :nested               :type/Dictionary} database-type))

;    leftover types from athena
;    :array                :type/Array
;    :varbinary            :type/*
;    :char                 :type/Text
;    :date                 :type/Date
;    :decimal              :type/Decimal
;    :int                  :type/Integer
;    :map                  :type/*
;    :string               :type/Text
;    :struct               :type/Dictionary
;    :timestamp            :type/DateTime
;    :varchar              :type/Text} database-type))

;; keyword function converts database-type variable to a symbol, so we use symbols above to map the types
(defn- database-type->base-type-or-warn
  "Given a `database-type` (e.g. `VARCHAR`) return the mapped Metabase type (e.g. `:type/Text`)."
  [driver database-type]
  (or (sql-jdbc.sync/database-type->base-type driver (keyword database-type))
      (do (log/warn (format "Don't know how to map column type '%s' to a Field base_type, falling back to :type/*."
                            database-type))
          :type/*)))

;; Need to override the next 2 functions as they use ResultSet/CLOSE_CURSORS_AT_COMMIT which isn't supported by ES Driver
(defmethod sql-jdbc.execute/prepared-statement :elasticsearch
  [driver ^Connection conn ^String sql params]
  (let [stmt (.prepareStatement conn sql
                                ResultSet/TYPE_FORWARD_ONLY
                                ResultSet/CONCUR_READ_ONLY
                                ResultSet/HOLD_CURSORS_OVER_COMMIT)]
    (try
      (try
        (.setFetchDirection stmt ResultSet/FETCH_FORWARD)
        (catch Throwable e
          (log/debug e (trs "Error setting result set fetch direction to FETCH_FORWARD"))))
      (sql-jdbc.execute/set-parameters! driver stmt params)
      stmt
      (catch Throwable e
        (.close stmt)
        (throw e)))))

(defmethod sql-jdbc.execute/connection-with-timezone :elasticsearch
  [driver database ^String timezone-id]
  (let [conn (.getConnection (sql-jdbc.execute/datasource database))]
    (try
      (try
        (.setHoldability conn ResultSet/HOLD_CURSORS_OVER_COMMIT)
        (catch Throwable e
          (log/debug e (trs "Error setting default holdability for connection"))))
      conn
      (catch Throwable e
        (.close conn)
        (throw e)))))
;; END ResultSet/CLOSE_CURSORS_AT_COMMIT -> ResultSet/HOLD_CURSORS_OVER_COMMIT override
