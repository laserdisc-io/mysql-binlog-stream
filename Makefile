install_plantuml:
	brew install plantuml

generate_pipes_diagram:
	plantuml -o /tmp docs/pipes.uml && open /tmp/pipes.png

.PHONY: help
help: ## Show this help message.
	@echo 'usage: make [target] ...'
	@echo
	@echo 'targets:'
	@egrep '^(.+)\:\ ##\ (.+)' ${MAKEFILE_LIST} | column -t -c 2 -s ':#'
