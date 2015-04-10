
function existy(x) { return x != null }

var trace = function() { console.log.apply(console, arguments); };
//var trace = function() {};

var onAir = true; // Are we broadcasting changes?

// Execute a block without broadcasting the change
function offAir(block) {
  var was = onAir;
  onAir = false;
  block();
  onAir = was;
}

var editor = ace.edit("editor");
editor.setTheme("ace/theme/merbivore");
editor.getSession().getDocument().setNewLineMode("unix");
editor.getSession().setMode("ace/mode/markdown");

// Convert ACE "position" (row/column) to WOOT "index":
function idx(position) {
  return editor.getSession().getDocument().positionToIndex(position);
}

// Covert a WOOT "index" into an ACE "position"
function pos(idx) {
  return editor.getSession().getDocument().indexToPosition(idx);
}

// Convert a WOOT operation to an ACE delta object for WOOT index i:
function asDelta(ch, isVisible, i) {
  return {
    action: isVisible ? "insertText" : "removeText",
    range: {
      start: pos(i),
      end:   pos(i + 1)
    },
    text: ch
  };
}

var updateEditor = function(ch, isVisible, visiblePos) {
  console.log("Updating Editor", ch, isVisible, visiblePos);
  var delta = asDelta(ch, isVisible, visiblePos);
  offAir(function() {
    editor.getSession().getDocument().applyDeltas([delta]);
  });
}

/* -- From client to server functions: local integration ----------------------------------------- */

// TODO: Make editor read-only until connected to server?

var client;
jQuery(document).ready(function() {
  client = new client.WootClient(updateEditor);
  wsInit();
});

// `text` is of arbitrary size. For now we serialize as individual operations:
var broadcast = function(op, text, range) {
  var base = idx(range.start), len = text.length;
  // When removing multiple character, the position for delete does not change:
  function charpos(p) { return op == "insertText" ? base+p : base; }

  for(var p=0; p<len; p++)
    broadcast1(op, text[p], charpos(p));
};

var broadcast1 = function(opType, ch, pos) {
  trace("Broadcasting: ",opType," on ",ch," @ ",pos);
  var op = opType === "insertText" ? client.insert(ch, pos) : client.delete(pos);
  trace("Back out to "+op);

  $('#lastCharSent').html(ch+" "+opType);
  doSend(op);
};

function cat(lines) {
  var nl = editor.getSession().getDocument().getNewLineCharacter();
  var combined =
   (_.chain(lines)
      .map(function(line) {return line+nl; })
      .reduce(function(acc,line) { return acc+line; })
      .value());
  return combined;
}

var aceCommands = {
  insertText:  function(text,range) { broadcast("insertText", text, range); },
  removeText:  function(text,range) { broadcast("removeText", text, range); },
  insertLines: function(text,range,event) { aceCommands.insertText(cat(event.data.lines), range); },
  removeLines: function(text,range,event) { aceCommands.removeText(cat(event.data.lines), range); }
};

function dispatch(f, text, range, event) {
  _.isFunction(f) ? f(text, range, event) : trace("Ignoring Command ",event.data.action);
}

editor.getSession().on('change', function(e) {
  $('#lastAceEvent').html(e.data.action);
  if (onAir) dispatch(aceCommands[e.data.action], e.data.text, e.data.range, e);
});