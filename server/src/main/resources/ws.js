/*

This is pretty much the http://websockets.org/ example code
with small changes.

*/
var wsUri = "ws://127.0.0.1:8080/woot/edit/default";
var statusIndicator, statusMsg;
var websocket;

function wsInit()
{
  statusIndicator = document.getElementById("wsStatus");
  statusMsg = document.getElementById("wsMsg");
  status("#ff9000", "Connecting...");
  websocket = new WebSocket(wsUri);
  testWebSocket();
}

function testWebSocket()
{
  websocket.onopen = function(evt) { onOpen(evt) };
  websocket.onclose = function(evt) { onClose(evt) };
  websocket.onmessage = function(evt) { onMessage(evt) };
  websocket.onerror = function(evt) { onError(evt) };
}

function onOpen(evt) {
  status("#00ff00", "Editing: default");
}

function onClose(evt) {
  // console.log(evt);
  status("#ff0000", "Connection Closed");
}

function onMessage(evt)
{
  // console.log(evt, JSON.parse(evt.data));
  client.ingest(evt.data);
}

function onError(evt) {
  status("#ff000", evt.data);
}

function doSend(message) {
  websocket.send(message);
}

function status(colour, msg) {
   statusIndicator.style.color = colour;
   statusMsg.innerHTML = msg;
}