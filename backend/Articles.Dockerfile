FROM golang:1.22.2-alpine3.19 AS builder
# create appuser.
RUN adduser --disabled-password --gecos '' elf
# create workspace
WORKDIR /opt/app/
COPY ./service_articles/go.mod ./service_articles/go.sum ./
RUN mkdir /opt/models
COPY ../models/go.mod ../models/go.sum ../models
RUN mkdir /opt/tools
COPY ../tools/go.mod ../tools/go.sum ../tools
# fetch dependancies
RUN go mod download # add verify after resolving issue with local models module
# copy the source code as the last step
COPY . .
# build binary
RUN pwd
RUN ls -alh
RUN ls -alh ./service_articles
RUN ls -alh ./models
RUN ls -alh ./tools
WORKDIR /opt/app/service_articles
RUN CGO_ENABLED=0 GOOS=linux go build -ldflags="-w -s" -a -installsuffix cgo -o /go/bin/ ./cmd/app

# build a small image
FROM alpine:3.14.1
LABEL language="golang"
LABEL org.opencontainers.image.source https://github.com/soniccat/WordTeacher
# import the user and group files from the builder
COPY --from=builder /etc/passwd /etc/passwd
# copy the static executable
COPY --from=builder --chown=elf:1000 /go/bin/app /app

COPY ./application_default_credentials.json ./
ENV GOOGLE_APPLICATION_CREDENTIALS "/application_default_credentials.json"
RUN chmod 777 "/application_default_credentials.json"

# use a non-root user
USER elf
# run app
ENTRYPOINT ["./app"]
