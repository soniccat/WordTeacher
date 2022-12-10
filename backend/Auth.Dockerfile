# golang alpine 1.15.6-alpine as base image
FROM golang:1.19.2-alpine AS builder
# create appuser.
RUN adduser --disabled-password --gecos '' elf
# create workspace
WORKDIR /opt/app/
COPY ./auth/go.mod ./auth/go.sum ./
RUN mkdir /opt/models
COPY ../models/go.mod ../models/go.sum ../models
# fetch dependancies
RUN go mod download # add verify after resolving issue with local models module
# copy the source code as the last step
COPY . .
# build binary
RUN pwd
RUN ls -alh
RUN ls -alh ./auth
RUN ls -alh ./models
WORKDIR /opt/app/auth
RUN CGO_ENABLED=0 GOOS=linux go build -ldflags="-w -s" -a -installsuffix cgo -o /go/bin/ ./cmd/app

EXPOSE 4000

# build a small image
FROM alpine:3.14.1
LABEL language="golang"
LABEL org.opencontainers.image.source https://github.com/soniccat/WordTeacher
# import the user and group files from the builder
COPY --from=builder /etc/passwd /etc/passwd
# copy the static executable
COPY --from=builder --chown=elf:1000 /go/bin/app /app
# use a non-root user
USER elf
# run app
ENTRYPOINT ["./app"]
