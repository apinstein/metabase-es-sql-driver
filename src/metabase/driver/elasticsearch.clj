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
            [metabase.util :as u])
  (:import [java.sql Connection ResultSet Types DatabaseMetaData Timestamp]
           (java.time OffsetDateTime ZonedDateTime)))

(driver/register! :elasticsearch, :parent :sql-jdbc)

;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                          metabase.driver method impls                                          |
;;; +----------------------------------------------------------------------------------------------------------------+

;(defmethod driver/supports? [:athena :foreign-keys] [_ _] true)
;
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
;; https://s3.amazonaws.com/athena-downloads/drivers/JDBC/SimbaAthenaJDBC_2.0.5/docs/Simba+Athena+JDBC+Driver+Install+and+Configuration+Guide.pdf
;(defmethod sql-jdbc.sync/database-type->base-type :athena [_ database-type]
;  ({:array      :type/Array
;    :bigint     :type/BigInteger
;    :binary     :type/*
;    :varbinary  :type/*
;    :boolean    :type/Boolean
;    :char       :type/Text
;    :date       :type/Date
;    :decimal    :type/Decimal
;    :double     :type/Float
;    :float      :type/Float
;    :integer    :type/Integer
;    :int        :type/Integer
;    :map        :type/*
;    :smallint   :type/Integer
;    :string     :type/Text
;    :struct     :type/Dictionary
;    :timestamp  :type/DateTime
;    :tinyint    :type/Integer
;    :varchar    :type/Text} database-type))


;; keyword function converts database-type variable to a symbol, so we use symbols above to map the types
(defn- database-type->base-type-or-warn
  "Given a `database-type` (e.g. `VARCHAR`) return the mapped Metabase type (e.g. `:type/Text`)."
  [driver database-type]
  (or (sql-jdbc.sync/database-type->base-type driver (keyword database-type))
      (do (log/warn (format "Don't know how to map column type '%s' to a Field base_type, falling back to :type/*."
                            database-type))
          :type/*)))

;(defmethod sql-jdbc.execute/connection-with-timezone :elasticsearch
;  [driver database ^String timezone-id]
;  (let [conn (.getConnection (datasource database))]
;    (try
;      (set-best-transaction-level! driver conn)
;      (set-time-zone-if-supported! driver conn timezone-id)
;      (try
;        (.setReadOnly conn true)
;        (catch Throwable e
;          (log/debug e (trs "Error setting connection to read-only"))))
;      (try
;        (.setHoldability conn ResultSet/HOLD_CURSORS_OVER_COMMIT)
;        (catch Throwable e
;          (log/debug e (trs "Error setting default holdability for connection"))))
;      conn
;      (catch Throwable e
;        (.close conn)
;        (throw e)))))

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
