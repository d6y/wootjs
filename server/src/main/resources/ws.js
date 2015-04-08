var wsUri = "ws://127.0.0.1:8080/edit/default";
var output;
var websocket;

function wsInit()
{
  output = document.getElementById("wsStatus");
  status("Connecting...");
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

function onOpen(evt)
{
  status("CONNECTED");
  //doSend("WebSocket is here");
}

function onClose(evt)
{
  console.log(evt);
  status("DISCONNECTED");
}

function onMessage(evt)
{
  //status('<span style="color: blue;">RESPONSE: ' + evt.data+'</span>');
  console.log(evt);
  console.log(JSON.parse(evt.data));
  client.ingest(evt.data);
 // websocket.close();
}

function onError(evt)
{
  status('<span style="color: red;">ERROR:</span> ' + evt.data);
}

function doSend(message)
{
  status("SENT: " + message);
  websocket.send(message);
}

function status(message) {
   output.innerHTML = message;
}

