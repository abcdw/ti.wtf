.PHONY: test

clean:
	rm -rf target/

server/uberjar:
	clojure -A:server:uberjar

server/nrepl:
	clojure -A:server:test:nrepl -m nrepl.cmdline --middleware '["refactor-nrepl.middleware/wrap-refactor" "cider.nrepl/cider-middleware"]'

server/test:
	clojure -A:server:test:test-runner

server/run:
	clojure -A:server -m ti.wtf.core

db/start:
	docker-compose up -d

db/stop:
	docker-compose stop

ui/watch:
	npx shadow-cljs watch :dev
