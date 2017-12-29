#Makefile

.PHONY: all
all: clean package docker

.PHONY: clean
clean:
	mvn clean

.PHONY: package
package:
	mvn package -DskipTests

.PHONY: docker
docker:
	docker build -t jenkins/pct .
