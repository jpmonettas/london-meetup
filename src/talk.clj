(ns talk
  (:require [flow-storm.api :as fs-api]
            [cljs.cli :as cli]
            [cljs.main :as cljs-main]
            [quil.core :as q]))

(do

  (defn local-connect []
    (fs-api/local-connect {:styles "/home/jmonetta/.flow-storm/big-fonts.css"}))

  (defn cljs-main [& args]
    (with-redefs [clojure.core/shutdown-agents (fn [] nil)]
      (apply cljs-main/-main args)))

  (def screen-width 400)
  (def screen-height 400)
  (def ball-size 20)
  (def speed 4)

  (def state (atom {:x 100
                    :y 300
                    :vx speed
                    :vy speed}))

  (defn update-ball-state [{:keys [vx vy] :as bs}]
    (let [bs' (-> bs
                  (update :x + vx)
                  (update :y + vy))]
      (cond-> bs'
        (<= (:x bs') 0)             (assoc :vx speed)
        (>= (:x bs') screen-width)  (assoc :vx (- speed))
        (<= (:y bs') 0)             (assoc :vy speed)
        (>= (:y bs') screen-height) (assoc :vy (- speed)))))

  (defn update-state [s]
    (-> s
        update-ball-state))

  (defn run-game []
    (q/defsketch graphics-utils
      :title "Breakout"
      :settings #(q/smooth 2)
      :setup (fn setup []
               (q/color-mode :rgb 255)
               (q/frame-rate 60)
               (q/background 190)
               (q/stroke 0 0 0)
               (q/fill   145 229 134))
      :draw (fn draw []
              (let [{:keys [x y]} (swap! state update-state)]
                (q/clear)
                (q/ellipse x y ball-size ball-size)))
      :size [screen-width screen-height])))













;;--------------------------------------------( FlowStorm )--------------------------------------------.







                                  Debugging and exploring Clojure applications

                                                with FlowStorm









;--------------------------------------------------------------------------------------------------------
















;;--------------------------------------( My journey to Clojure )--------------------------------------.




                                        - Writing software since 90s

                                        - C/C++ and Java

                                        - CommonLisp in 2004

                                        - Clojure since 2011, full time

                                        - Also ClojureScript and web stuff






;--------------------------------------------------------------------------------------------------------


















;;----------------------------------------------( Agenda )---------------------------------------------.




                                        - Debuggers and Clojure

                                        - Tracing debuggers

                                        - Debugging Clojure with FlowStorm (live demo)

                                        - FlowStorm internals

                                        - Known limitations

                                        - Q&A




;--------------------------------------------------------------------------------------------------------



















;;-----------------------------------( Debuggers and Clojure (1/7) )-----------------------------------.



                         Clojure has the repl, does it need a debugger?

                         https://cognitect.com/blog/2017/6/5/repl-debugging-no-stacktrace-required
                                                  by Stu Halloway

                         Strategy :


                             - blindly subdivide the problem (use def and eval from editor)

                             - execute the code in your head

                             - mix and match



;--------------------------------------------------------------------------------------------------------
















;;-----------------------------------( Debuggers and Clojure (2/7) )-----------------------------------:




                                     ;; define locals globaly
                                     #_(def n 24)


                                     (defn foo [n]
                                        (cond (> n 40) (+ n 20)
                                              (> n 20) (- (first n) 20)
                                              :else 0))







;--------------------------------------------------------------------------------------------------------


















;;-----------------------------------( Debuggers and Clojure (3/7) )-----------------------------------.



                          Debugging with the repl is great but :

                                 - def isn't always easy (even with "scope capture")

                                 - loops

                                 - nil-punning (finding where nils come from)

                                 - clojure is dynamic (hard to reason without seeing a shape)


                          A debugger can improve further the already amazing experience
                          of repl debugging



;--------------------------------------------------------------------------------------------------------



















;;-----------------------------------( Debuggers and Clojure (4/7) )-----------------------------------.




                    - Cider               (https://github.com/clojure-emacs/cider)
                    - Calva debugger      (https://github.com/BetterThanTomorrow/calva)
                    - Cursive             (https://github.com/cursive-ide/cursive)
                    - FlowStorm           (https://github.com/jpmonettas/flow-storm-debugger)
                    - Debux               (https://github.com/philoskim/debux)
                    - Sayid               (https://github.com/clojure-emacs/sayid)
                    - Postmortem          (https://github.com/athos/postmortem)
                    - clojure.tools.trace (https://github.com/clojure/tools.trace)
                    - scope-capture       (https://github.com/vvvvalvalval/scope-capture)
                    - Portal              (https://github.com/djblue/portal)
                    - Reveal              (https://github.com/vlaaad/reveal)
                    - and more...



;--------------------------------------------------------------------------------------------------------


















;;--------------------------------( Debuggers and Clojure: How? (5/7) )--------------------------------.






                            - JDI (Java Debugging Interface) over JTI (jvmti.h)



                            - Instrument by code rewriting








;--------------------------------------------------------------------------------------------------------



















;;------------------------( Debuggers and Clojure: JPDA instrumentation (6/7) )------------------------.




                        JDI (Java Debugging Interface) or JTI (jvmti.h)

                        Examples: Cursive (no source code so guessing)

                        Pros:
                              - Instrument at the JVM level, can work with Java also

                        Cons:
                              - JVM debugging is line and place oriented
                              - .class LineNumberTable support only line





;--------------------------------------------------------------------------------------------------------


















;;--------------------( Debuggers and Clojure: rewrite code instrumentation (7/7) )--------------------.




                                      Instrument code by rewriting it

                                      Examples: Cider, Calva, FlowStorm, Debux, ...

                                      Pros:
                                            - Nice expression debugging
                                            - Trace or block
                                            - Clojure aware

                                      Cons:
                                            - Not as efficient




;--------------------------------------------------------------------------------------------------------
































;;-------------------------------------( Tracing debuggers (0/4) )-------------------------------------.








                                           Tracing debuggers










;--------------------------------------------------------------------------------------------------------




























;;-------------------------------------( Tracing debuggers (1/4) )-------------------------------------.


                                       1. Instrument code for tracing
                                       2. Run it
                                       3. Collect traces
                                       4. Visualize / Explore

                            Pros:
                                  - time travel
                                  - multiple visualizations (including single stepping)

                            Cons:
                                  - resource intensive if blindly tracing everything

                            Other examples: https://rr-project.org
                            (low level tracing debugger for C, C++, Rust, etc)



;--------------------------------------------------------------------------------------------------------

















;;-------------------------------------( Tracing debuggers (2/4) )-------------------------------------.






                              Implemented in two parts:

                              - Instrumentation and tracing (com.github.jpmonettas/flow-storm-inst)

                              - Trace analysis (com.github.jpmonettas/flow-storm-dbg)








;--------------------------------------------------------------------------------------------------------




















;;-------------------------------------( Tracing debuggers (3/4) )-------------------------------------.




                                 FlowStorm instrumented code will trace :

                                     - Function Calls (with args)

                                     - Function returns

                                     - Bounded values (fn-args, let, loop, etc)

                                     - All expressions return values






;--------------------------------------------------------------------------------------------------------
















;;-------------------------------------( Tracing debuggers (4/4) )-------------------------------------.





                         Q: Isn't tracing everything expensive?

                         A: Don't serialize, retain pointers and snapshot references



                         Q: Why aren't tracing debuggers common in other languages?

                         A: Clojure is special (immutability, expression based, easy to instrument)





;--------------------------------------------------------------------------------------------------------



























;;----------------------------------------( FlowStorm overview )---------------------------------------.




                               - A Clojure and ClojureScript debugger

                               - Instruments by rewriting

                               - Tracing debugger (non blocking)

                               - Meant for development, not production

                               - Designed to improve repl debugging experience

                               - IDE independent, but can be integrated




;--------------------------------------------------------------------------------------------------------























;;-------------------------------( Examples: Connect the debugger (1/10) )-----------------------------:









                                             (local-connect)









;------------------------------------------------------------------------------------------------------















;;-------------------------( Examples: Run any expression instrumented (2/10) )------------------------:




                                         #rtrace
                                         (->> (range 70)
                                              (filter odd?)
                                              (partition-all 2)
                                              (map second)
                                              (drop 10)
                                              (reduce +))


                             Nil-punning, find where is nil coming from?

                             Good for teaching and learning



;--------------------------------------------------------------------------------------------------------



























;;-------------------------------( Examples: Exception debugging (3/10) )------------------------------:


                                        From Stuart Halloway blog post

                  (https://cognitect.com/blog/2017/6/5/repl-debugging-no-stacktrace-required)



                                        #trace
                                        (defn foo [n]
                                           (cond (> n 40) (+ n 20)
                                                 (> n 20) (- (first n) 20)
                                                 :else 0))


                                        (foo 24)



;--------------------------------------------------------------------------------------------------------






























;;-------------------------------------( Examples: Flow ids (4/10) )-----------------------------------.

[[../images/flow-ids.png]]
;--------------------------------------------------------------------------------------------------------


















;;-------------------------------( Examples: Libraries and loops (5/10) )------------------------------:





                                 (require '[clojure.data.codec.base64 :as b64])


                                 (-> "London Clojure Meetup"
                                     .getBytes
                                     b64/encode
                                     (String.))







;--------------------------------------------------------------------------------------------------------















;;----------------------------------( Examples: Multithreading (6/10) )--------------------------------:







                                       #rtrace
                                       (->> (range 5)
                                            (pmap (fn square [i]
                                                    (* i i)))
                                            (reduce +))







;--------------------------------------------------------------------------------------------------------
















;;---------------------------------( Examples: Entire codebases (7/10) )-------------------------------:







                   #rtrace
                   (cljs-main "-t" "nodejs" "/home/jmonetta/demo/src/org/foo/myscript.cljs")










;--------------------------------------------------------------------------------------------------------

















;;---------------------------------( Examples: Entire codebases (8/10) )-------------------------------.



                                 Jumping through time and reverse engineering tools :


                                          - The call tree

                                          - Searching around

                                          - Defing for the repl (values and locals)

                                          - Functions






;--------------------------------------------------------------------------------------------------------

















;;-------------------------------( Examples: Conditional tracing (9/10) )------------------------------:


                           (defn update-ball-state [{:keys [vx vy] :as bs}]
                             (let [bs' (-> bs
                                           (update :x + vx)
                                           (update :y + vy))]
                               (cond-> bs'
                                 (<= (:x bs') 0)             (assoc :vx speed)
                                 (>= (:x bs') screen-width)  (assoc :vx (- speed))
                                 (<= (:y bs') 0)             (assoc :vy speed)
                                 (>= (:y bs') screen-height) (assoc :vy (- speed)))))


                           (defn update-state [state] ;; called 60 times per second
                             (-> state update-ball-state))


                           (run-game)

;--------------------------------------------------------------------------------------------------------
















;;-------------------------------( Examples: Conditional tracing (10/10) )-----------------------------:


                    #trace (defn update-ball-state [{:keys [vx vy] :as bs}]
                             (let [bs' (-> bs
                                           (update :x + vx)
                                           (update :y + vy))]
                               (cond-> bs'
                                 (<= (:x bs') 0)             (assoc :vx speed)
                                 (>= (:x bs') screen-width)  (assoc :vx (- speed))
                                 (<= (:y bs') 0)             (assoc :vy speed)
                                 (>= (:y bs') screen-height) (assoc :vy (- speed)))))


                   #ctrace (defn update-state [state] ;; called 60 times per second
                             ^{:trace/when (<= (:x state) 0)}
                             (-> state update-ball-state))

                           (run-game)

;--------------------------------------------------------------------------------------------------------

















;;----------------------------( FlowStorm internals: Instrumentation (1/3) )---------------------------:





                                    Instrumenting a form:


                                    (-> form
                                        tag-form-recursively    ; tag with coordinates
                                        macroexpand-all         ; to get rid of macros
                                        instrument-recursively)







;--------------------------------------------------------------------------------------------------------




















;;----------------------------( FlowStorm internals: Instrumentation (2/3) )---------------------------.

 [[../images/instr-expansion.png]]
;------------------------------------------------------------------------------------------------------












;;-----------------------------( FlowStorm internals: Architecture (3/3) )-----------------------------.

  [[../images/frame-tree-indexer.png]]

;--------------------------------------------------------------------------------------------------------
























;;----------------------------------------( Known limitations )----------------------------------------.





                           - Big forms can't be fully instrumented    [downgrades to :light]

                           - (fn [] (recur))   <without loop>         [skip]

                           - (fn foo [] (lazy-seq (... (foo))))       [skip]









;--------------------------------------------------------------------------------------------------------

























;;--------------------------------( ClojureScript CURRENT limitations )--------------------------------.




                                 - No browser :(

                                 - No namespaces or var instrumentation :(




                            Can probably be implemented by connecting to a REPL :-)







;--------------------------------------------------------------------------------------------------------





















;;---------------------------------------( Ideas for the future )--------------------------------------.





                              - Better value inspector (like REBL, Reveal, Portal)


                              - Execution derived types and documentation


                              - Integration with async-profiler, JOL, and more ...







;--------------------------------------------------------------------------------------------------------




















;;----------------------------------------------( Thanks )---------------------------------------------.






                                            - Try it out!

                                            - Contribute :

                                                 - Github issues
                                                 - Github discussions
                                                 - Slack (#flow-storm)






;--------------------------------------------------------------------------------------------------------

































;;-----------------------------------------------( END )-----------------------------------------------.








                                                  Q & A










;--------------------------------------------------------------------------------------------------------




































END
