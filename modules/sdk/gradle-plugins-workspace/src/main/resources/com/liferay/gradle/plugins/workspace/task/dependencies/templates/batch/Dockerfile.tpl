FROM bash:latest

COPY /batch /batch
COPY job.sh /batch/job.sh

RUN \
	apk add --no-cache curl jq tree && \
	chmod +x /batch/job.sh

WORKDIR /batch/

ENTRYPOINT ["/batch/job.sh"]