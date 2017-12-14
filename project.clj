(defproject pt-viz "1.0.0"
  :description "Script to generate PT epic's dependency graph"
  :url "http://github.com/reifyhealth/pt-viz"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.cemerick/url "0.1.1"]
                 [cheshire "5.8.0"]
                 [clj-http "3.7.0"]]
  :main ^:skip-aot pt-viz.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
