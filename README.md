# Artur

A chatbot for initiating torrent downloads. Uses [Twilio](https://www.twilio.com/docs/sms) for sending and receiving SMS messages, and [rTorrent](https://rtorrent-docs.readthedocs.io/en/latest/) for downloading.

## Requirements

* Twilio account, with some webhook setup required
* A purchased Twilio number, for receiving texts
* Docker

## How it works

1. You send a torrent URL to your Twilio number in a message like `Add movie https://example.com/starwars.torrent`
2. Twilio forwards it to your configured webhook (one you've set up in advance)
3. The app sends a confirmation response and the download begins
6. The app sends updates every $x minutes (another env var) as the download progresses ??? and a final update once it has finished
