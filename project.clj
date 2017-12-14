(defproject pt-viz "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.cemerick/url "0.1.1"]
                 [cheshire "5.8.0"]
                 [clj-http "3.7.0"]]
  :main ^:skip-aot pt-viz.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
