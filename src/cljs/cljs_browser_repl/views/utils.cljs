(ns cljs-browser-repl.views.utils
  (:require-macros [re-com.core :refer [handler-fn]])
  (:require [cljs.pprint :as pprint]
            [clojure.string :as string]
            [clojure.walk :as walk]
            [reagent.core :as reagent]
            [re-com.core :refer [popover-tooltip]]
            [hickory.core :as hickory]
            [goog.string :as gstring]))

(def clojuredocs-url "http://clojuredocs.org/")

(defn strip-namespace
  "Given a cljs symbol, strip the namespace part."
  [sym]
  (string/join (drop-while #(re-find #"\." %) (string/split (str sym) #"/" 2))))

(defn symbol->clojuredocs-url
  "Given a cljs symbol (with fully qualified ns or without, in which
  case it defaults to clojure.core), returns the url to ClojureDocs
  documentation."
  [cljs-symbol]
  (let [[ns symbol] (string/split (str cljs-symbol) #"/" 2)]
    (str clojuredocs-url (if ns
                           (string/replace ns #"cljs" "clojure")
                           "clojure.core")
         "/" symbol)))

(defn number->word
  "From: http://clojurescriptmadeeasy.com/blog/how-to-humanize-text-cl-format.html
  If (< 0 n 10) use the ~R directive, otherwise use ~A"
  [n]
  (pprint/cl-format nil "~:[~a~;~r~]" (< 0 n 10) n))

(defn calculate-popover-position
  "Calculates the tooltip orientation for a given symbol."
  [[x y]]
  (let [h (.-innerHeight js/window)
        w (.-innerWidth  js/window)
        v-threshold (quot h 2)
        v-position  (if (< y v-threshold) "below" "above")
        h-threshold-left (quot w 3)
        h-threshold-cent (* 2 h-threshold-left)
        h-position (cond
                    (< x h-threshold-left) "right"
                    (< x h-threshold-cent) "center"
                    :else "left")]
    (keyword (str v-position \- h-position))))

(defn unescape-html
  "Walk a given Hiccup form and remove all pure whitespace."
  [hiccup]
  (walk/prewalk
   (fn [form]
     (if (string? form)
       (goog.string/unescapeEntities form)
       form))
   hiccup))

(defn trim-strings
  "Walk a given Hiccup form and remove all pure whitespace."
  [hiccup]
  (walk/prewalk
   (fn [form]
     (if (string? form)
       (string/trim form)
       form))
   hiccup))

(defn html-string->hiccup
  "Convert the string to hiccup vector, filling up class style and attr
  if present on the initial :div"
  [html-string & {:keys [class style attr]}]
  (into [:div (merge {:class class
                      :style style}
                     attr)]
        (map (comp trim-strings unescape-html hickory/as-hiccup) (hickory/parse-fragment html-string))))

;; AR - Not used anywhere atm
(defn inject-attributes
  "Walk a given Hiccup form and inject the attribute map, merging (with
  precedence to attr-map) with the pre-existing one if any."
  [attr-map hiccup]
  (println hiccup)
  ;; having fun
  (assoc hiccup 1 (merge-with merge (when (map? (nth hiccup 1))
                                      (nth hiccup 1))
                              attr-map)))
