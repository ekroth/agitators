var ip = "ws://echo.websocket.org";
//var ip = "ws://192.168.1.2:3000";
var output;
var input;
var nick;
function init() { input = document.getElementById("input");
  input.addEventListener("keydown",inputListener,false);
  output = document.getElementById("output");
  nick = document.getElementById("nick");
  testSocket();
}

function inputListener(key) {
  var keyCode = key.keyCode;
  if (keyCode == 13) {
    send(nick.value, input.value);
    input.value="";
  } 
}

function testSocket() {
  try {
    socket = new WebSocket(ip);
  } catch (e) {
    write("error","red","fail");
  }
  socket.onopen = function(evt) {onOpen(evt);};
  socket.onclose = function(evt) {onClose(evt);};
  socket.onmessage = function(evt) {onMessage(evt)};
  socket.onerror = function(evt) {onClose(evt)};
}


function onOpen(evt) {
  write("onOpen", "green", "connected to "+ip);
}

hexHashCode = function(s){
  return Math.abs(+(s.split("").reduce(function(a,b){a=((a<<5)-a)+b.charCodeAt(0);return a&a},0)) % 4095).toString(16);              
}

function onMessage(evt) {
  try {
    data = JSON.parse((evt.data));
    write(data.nick, "#"+hexHashCode(data.nick), data.msg);
  }catch(err){
    write("error", "red", err + "<br><b>data</b>: " + evt.data);
  }
}

function onClose(evt) {
  write("onClose", "orange", evt.data);
}

function onError(evt) {
  write("onError", "red", evt.data);
}

function send(nick, msg) {
  socket.send('{"msg":"'+msg+'","nick":"'+nick+'"}');
}


function write(name, color, text) {
  var pre = document.createElement("p");
  pre.style.wordWrap = "break-word";
  var span = document.createElement("span"); 
  span.style.color = color; 
  span.innerHTML = "<b>"+name+"</b>: ";
  pre.appendChild(span)
  pre.innerHTML += text; 
  output.appendChild(pre);
  output.scrollTop = output.scrollHeight;
}

window.addEventListener("load",init,false);
