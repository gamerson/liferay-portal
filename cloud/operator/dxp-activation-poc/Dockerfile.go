FROM gcr.io/distroless/static:nonroot

ARG BINARY

COPY ${BINARY} /app

ENTRYPOINT ["/app"]

USER 65532:65532
