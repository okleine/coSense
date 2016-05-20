## COSENSE - A CoAP Server for Android

<img align="right" src="https://media.itm.uni-luebeck.de/people/kleine/cosense-screenshots/cosense_screenshot2.png" width="250"/>

COSENSE is based on [nCoAP](https://github.com/okleine/nCoAP). COSENSE is a CoAP Server to provide the actual sensor values from build-in sensors as Web Resources.

Currently, the implementation supports location, ambient pressure, ambient noise, and ambient brightness. Each resource can be activated or deactivated indenpendent from others.

Optionally, it is possible to register at a [Smart Service Proxy](https://github.com/okleine/smart-service-proxy) which enables the values to be accessible for interested clients dispite changing and/or private resp. [NAT](https://en.wikipedia.org/wiki/Network_address_translation)ed IP addresses of the smartphone.

See also [SPITFIREFOX - A Coap Client for Android](https://github.com/okleine/spitfirefox)
