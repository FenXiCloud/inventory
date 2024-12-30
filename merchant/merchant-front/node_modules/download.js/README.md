# download.js

[![NPM version][npm-image]][npm-url] [![Downloads][downloads-image]][npm-url] [![Dependency status][david-dm-image]][david-dm-url] [![Dev Dependency status][david-dm-dev-image]][david-dm-dev-url]

A simple file downloader for JavaScript. 

## Installation
```
npm install --save download.js
```

## Usage
Download text as file using the `downloadText` function:
```javascript
import { downloadText } from 'download.js'

downloadText('file.txt', 'Lorem ipsum dolor sit amet')
```
To download a blob, one may use the `downloadBlob` function.

[npm-url]: https://npmjs.org/package/download.js
[downloads-image]: http://img.shields.io/npm/dm/download.js.svg
[npm-image]: http://img.shields.io/npm/v/download.js.svg
[david-dm-url]:https://david-dm.org/inker/download.js
[david-dm-image]:https://david-dm.org/inker/download.js.svg
[david-dm-dev-url]:https://david-dm.org/inker/download.js#info=devDependencies
[david-dm-dev-image]:https://david-dm.org/inker/download.js/dev-status.svg
