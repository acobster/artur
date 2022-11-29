# Artur

A chatbot for initiating torrent downloads. Uses [Twilio](https://www.twilio.com/docs/sms) for sending and receiving SMS messages, and [Deluge CLI](https://deluge-torrent.org/) for downloading. Written in Clojure.

## Requirements

* Twilio account, with some webhook setup required
* A purchased Twilio number, for receiving texts
* A recent version of Java
* Deluge

## How it works

1. You send a torrent URL to your Twilio number in a message like `Add movie https://archive.org/download/BigBuckBunny_124/BigBuckBunny_124_archive.torrent`
2. Twilio forwards it to your configured webhook (one you've set up in advance)
3. The app sends a confirmation response and the download begins
6. The app sends updates every $x minutes (another env var) as the download progresses ??? and a final update once it has finished
