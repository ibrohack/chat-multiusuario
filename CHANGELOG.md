# Changelog

Notable changes per release, grouped by [Conventional Commits](https://www.conventionalcommits.org/) type.

## 1.1.0 — 2026-11-01

Advanced user interface and live view switching.

### Features
- **client:** add view selector and live view switching
- **advanced-ui:** split general and private chats into tabs

### Documentation
- explain the advanced UI

## 1.0.0 — 2026-11-01

Rubric-complete chat: console server and JavaFX client with the classic view.

### Features
- **core:** define protocol packets
- **core:** add object channel over sockets
- **server:** add synchronized chat room
- **server:** parse configuration from command-line arguments
- **server:** accept clients with one thread per connection
- **server:** broadcast public messages and deliver private ones
- **server:** add statistics monitor thread
- **client:** connect to the server with sender and receiver threads
- **client:** add chat session model shared by both views
- **classic-ui:** send and display public and private messages

### Documentation
- explain how to run and use the classic UI
- document architecture and technical decisions

### Build
- convert to multi-module build and upgrade dependencies
- run javadoc doclint on all packages

### Chores
- **client:** remove archetype secondary view
- add commit-msg hook for conventional commits
- track project docs and ignore logs and plans
