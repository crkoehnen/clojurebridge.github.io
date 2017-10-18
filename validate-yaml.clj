#!/usr/bin/env lumo

(require '[cljs.nodejs :as nodejs])
(require '[clojure.spec.alpha :as s])
(require '[clojure.string :as st])
(def yml (nodejs/require "js-yaml"))
(def fs (nodejs/require "fs"))

(def datetime-regex #"\d\d\d\d-\d\d-\d\d \d\d:\d\d:\d\d [+-]\d\d\d\d")
(s/def ::date (s/and string? #(re-matches datetime-regex %)))

(def email-regex #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$")
(s/def ::email (s/and string? #(re-matches email-regex %)))
(s/def ::url (s/and string? #(not (st/blank? %))))

(s/def ::name string?)
(s/def ::names (s/coll-of ::name))
(s/def ::organizers (s/keys :req-un [::email ::names]))

(s/def ::layout #(= "post" %))

(s/def ::street string?)
(s/def ::city string?)
(s/def ::country string?)
(s/def ::city_image_url ::url)

(s/def ::workshop_dates string?)

(s/def ::latitude (s/double-in :min -180.0 :max 180.0))
(s/def ::longitude (s/double-in :min 0 :max 90.0))

(s/def ::image ::url)
(s/def ::sponsor (s/keys :req-un [::name ::url ::image]))
(s/def ::sponsors (s/coll-of ::sponsor))

(s/def ::service string?)
(s/def ::username string?)
(s/def ::account (s/keys :req-un [::service ::username]))
(s/def ::accounts (s/coll-of ::account))

(s/def ::workshop-metadata (s/keys :req-un [::accounts
                                            ::city
                                            ::city_image_url
                                            ::country
                                            ::date
                                            ::latitude
                                            ::layout
                                            ::longitude
                                            ::organizers
                                            ::sponsors
                                            ::street
                                            ::workshop_dates]))
(defn validate [k o]
  (let [valid (s/valid? k o)]
    (when (not valid) (s/explain k o))
    valid))

(defn read-file [f]
  (-> (.readFileSync fs f "utf8")
      (st/split "---\n")
      second))

(defn parse-yml [s]
  (-> (.safeLoad yml s)
      (js->clj :keywordize-keys true)))

(defn check-file [f]
  (->> (read-file f)
       parse-yml
       (validate ::workshop-metadata)))

(defn read-dir [dir]
  (let [files (-> (.readdirSync fs dir "utf8") js->clj)
        valid (every? #(check-file (str dir "/" %)) files)]
    (js/process.exit (if valid 0 1))))

(read-dir "_posts")
