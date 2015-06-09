# Towards Browser and Server Utopia with Scala.js: an example using CRDTs

- 13:20 -> 14:05
- 45 minutes

## Abstract

This session demonstrates the practical application of Scala.js using the example of a collaborative text editing algorithm, written once in Scala, but used from the JVM and JavaScript.

Scala.js is a compelling way to build JavaScript applications using Scala. It also addresses an important and related problem, namely using the same algorithm client-side and server-side.

After this session you'll have an appreciation of:

 - where Scala.js can help with mixed environment projects;
 - some of the gotchas you might encounter; and
 - an understanding of collaborative text editing and CRDTs.

This session is relevant to anyone wanting to execute Scala code in JavaScript. In particular I'll focus on exposing Scala code for use from JavaScript, rather than a complete application written solely in Scala. This mitigates the risk of adopting Scala.JS, while still benefiting from shared code usage.

The demonstration will be based around a CRDT. CRDTs are an important class of algorithms for consistently combining data from multiple distributed clients. As such they are a great target for Scala.js: the algorithms and data-structures involved will typically need to run on browsers and servers and we'd like to avoid implementing the (moderately complex) code twice. The specific algorithm will be WOOT, a text CRDT for correctly combining changes (think: Google Docs).＂

----

# Script

## Welcome

Hi Everyone - thanks for coming along.

I'm Richard. I work for Underscore.

Underscore is an consultancy, which covers quite a wide range of things, but I mostly wotk with customers by writing software and reviewing software.

This is a session on Scala.js

I'll be describing what I think of as a kind of ideal world for web dev. And show you how that looks, in practice.


By ideal, what I mean is...

- being able to writing in a languages with types, like Scala;
- I want to be able to run that code, on the server, in a fast and reliable environment, and the JVM is a good choice there;
- it means it has to run in a JavaScript environment, because that's everywhere; and
- I want to do this without repeating myself: I don't want to worry about marshaling data between those two places, and I do want to share code where we can.

Those are all things Scala.js can do, and we'll see.

## Change

The point of this, btw, the bigger picture, and why I think these things are desirable, is to make change easier.  Across the lifetime of an application, I want to use tools & techniques that make it quick and safe to make changes.

Scala.js can play a big part in making that a reality.

## Agenda

The talk is in three parts.

I'm not assuming any knowledge of Scala.js, so we'll spend a few minutes saying what Scala.js

I'll then introduce the example I'm using, which is collaborative text editing. So something like a Google Docs. That's the kind of app we'll be building. This is fun because it involves client-side stuff, server-side stuff, and an opportunity to write code once and us it browser side and server side. It demonstrates some nice parts of Scala.js.

It's also an example of a class of algorithm called a CRDT, which in themselves are interesting and important. We'll get to all of that in part 2.

The third part, is looking at the mechanics of how you get all this with Scala.js -- there's where most of the code will be.

## Ideas

As we go through this, I hope to share two ideas with you.

The first is that Scala.js is something you can gradually add to a project.

I hope you've seen some of the superb demos of Scala.js where complete games are built, and it's all in Scala and it runs in the browser. If you've not, search for them because they are a hugely impressive.

The focus here, though, is not to do that.  I want to gradually add Scala.js

This is important if you have a mixed team, where you've already have some great JavaScript developers and existing JavaScript assets. You don't want to throw any of that away. (You may want to throw it away, but I'm staying you don't have to).

Also: It's not often realistic to re-write everything, or you're not always starting from nothing.


The other idea is that Scala.js is pretty much ideal for the kinds of interactive, real-time, algorithms you might be working with. That will make more sense when I describe the collaborative editing app. And you'll see then while I'm so keen on Scala.js.

---

# PART 1

So let's look at what Scala.js is.

## scala-js.org

What you'll want to do, if this is all new to you, is get over to scala-js.org.

There's a good tutorial, and a set of pages to take you though the details of what it can do.

As it says there, scala.js a Scala to JavaScript compiler.


## You app.

So this means...

You write Scala code, and there's a plugin -- you can add to sbt like any other plugin -- that emits JavaScript code.  You can include that on a HTML, for example, and have it run.

Your application probably builds the Scala standard library, and maybe some other libraries that you depend on. And the plugin can deal with that. The standard library is there, and more pure Scala libraries are becoming available.

And in a browser you may want to access the DOM, and call JavaScript libraries like jQuery.  That's fine too, as the Scala.js has libraries that include typed bindings for the DOM and various JavaScript frameworks.

## Yes

What I found surprising about this, is that it covers the whole of the Scala language. There are some differences -- the JVM isn't JavaScript after all -- and we'll see some of those when we get into the code.

You don't get Java... It's a Scala to JavaScript compiler, after all. So don't expect JUnit to work or Scala libraries that depend on JUnit to work.

But that still gives you a lot to play with.

For example. yes, you can use ScalaCheck, shapeless, Scalaz. These projects publish artifacts you can depend on in your own projects to run in the browser or the server.

There are also new libraries appearing, specifically targeting the JVM and JavaScript.  Examples are Scalatags, which is a DSL for creating markup. microtest is unit testing framework, and micro pickle is a JSON serialization tool.

And as I mentioned there are typed bindings for the DOM, for jquery, for react and angular... these are, are believe, community maintained.

## So how do you use it?

In practice, Scala.js is pretty straight forward.

You add a plugin to your build
you go write some Scala code
You mark the entry point or points with an annotation. That allows Scala.js to figure out what it needs to include, and throw away a lot of the standard library that you don't use.
There's a command in SBT to generate the JavaScript, and then you include that file.

## You might be wondering what the JavaScript produced is like.

If we take this example of mapping over an option.  

The JavaScript output is, not exactly this, but this kind of thing.  You can see in Javascript there's an Option, and we construct one with apply. If it's empty, the answer is None; otherwise we get the value, compute the + 1 and give that back a an Some.

The real Javascript is not much bigger.  The names are longer and weirder. And there is more going on, but I just wanted to show the kinds of output you get.  

I don't normally go look at this output, but it's good to know it's reasonable stuff.

## Features

In summary...

You get a nice fast compiler, and one that's slower but produces more compact JavaScript.  For this collaborative editor we get 295k before any kind of gzip-like compression. That might be big or small for you, depending on what you're doing. You can decide if those sorts of sizes are going to work for you. It's absolutely fine for me.

The JavaScript itself is quick, and importantly, we get great interop with Javascript, which is more of what I'm interested in.

And another feature of Scala.js is there's an active community.  If you drop into the gittr room, you'll see what I mean by active.

## Framework

An important point of Scala.js is that it's not a framework.  What I mean is, it's not expecting you to use JavaScript in any particular way. It's not like GWT, and it's not anything like Angular.  It's a Scala to Javascript compiler. How you use that, is up to you.


----

# PART 2

Now the way we're going to use it is to build a collaborative text editing application. The app already exists, so we're going to reuse what we have, and get a mix of existing JavaScript and Scala.js code. And see some examples of calling between the two worlds.

# This is the kind of thing we're going to build

There's a text editor, and it happens to be using an existing JavaScript library called Ace.

But of course we have more than one participant sharing a document. That's what makes it collaborative.

And when we say sharing a document, the reality is that everyone has their own copy of the document. We're not talking about screen sharing here.

As I make edits my copy changes, it diverges from your copy.  And the trick is to share edits and make sure we all end up with consistent text.

So we're going to have a server. It's where you join to get a document, and it's the distribution point out to everyone.  It also means, we can do things on the server like lazily save a document.  And in this case the server technology is http4s .... although anything that can do a "chat" demo, could sit there.

So I make an edit, and it is sent to the server.  The server interprets that edit, and pass it on.  And the other two participants also consume the same edit.  The server and the client have to do the same thing to keep up to date with the edits being made.  And that is why Scala.js is a great fit for this, because we can share the code that consumes edits.

The question is, what are these messages that of these edits being sent around the network?

# Problem

You might think you can just thrown simple changes around the network, but that doesn't work out well.

As an example here are two people working on a shared document. Alice and Bob, are taking a day off from cryptography, and they both working on a document that contains the word cat.

What Alice is going to do is insert a H at position 2. So her document reads "CHAT".
Meanwhile, Bob has deleted this third character.

Now the network catches up, and they exchange edits.

Alice's insert at position 2 is kind of fine for Bob, but when Alice applies Bob's change... well the character at position 3 has changed, and an A gets deleted. Not a T.

Now each party has different documents, and no indication anything has gone wrong.

So you need something more sophisticated than that.

## CRDT

This is where CRDTs come into the picture.

We have replicated data in this situation: everyone has their own copy of a document.

And the commutative part is that, when we exchange changes, and those changes combine to produce the same results everywhere. Like addition is commutative: it doesn't matter what order the numbers arrive in, if you add them all up you get the same answer.

## WOOT Solution

There is this algorithm called WOOT that solves our problem.

You don't need to understand it for this talk, just that it has a couple of  signatures that were going to have to implement.

However the algorithm is interesting in its own right and an example of a CRDT so here's a quick overview...

Let's look at the same editing scenario.

Starting at the same place, this time Alice's change is to insert H between C and A. It's the same edit, but expressed differently -- last time we talked about absolute positions. Here we are talking about relations between characters.

 And Bob's change is to delete the T.  Now, the T isn't literally any letter T. Each character in a WOOT document has more structure -- which we'll see on the next slide.

Now when the edits are exchanged we see that Alice's edit applies fine for Bob: there is a C and an A, so the H goes in between.  And Bob's edit works for alice, because it asks to delete T and there is a T to delete.

Both end up with the same, consistent document.


## Representation

So what are these characters been passed around.  Each character, which we call a WChar for WOOT character, consists of five parts.

First every character has a unique ID.  That's made up of some global unique part, like a username, and a local clock. Some kind of counter that increments.  Each time I press a key and create a WChar, the clock increments, and the two parts combined give a globally unique ID for the character.

When we say we're inserting something between something else, that's the role of previous and next part of the WChar.  They are also character IDs.  So when we say insert H between C and A. We're talking about IDs.

Of course we have the character itself, alpha.

And finally nothing gets deleted from a WOOT document.  Individual characters are simply marked as invisible.

I'm showing you this, because we're going to model it as a case class later.

## Algorithm

The data structure is one part of the story. The algorithm is the other.

I'm not going to go into detail on the algorithm, but here's a quick illustration for how it works.

Here we have three people working on a document. The document contains abcd.  And this forms a graph of a goes to b, goes to c, goes to D.

The first person is inserting an X between C and D. And that modifies the graph, so it reads a b c x d.

The second person deletes the B, which we just hide.

And the third person inserts a y between b and c, which gives us this final graph. And we can do that insert because b, although invisible, exists in the data structure.

And we can read this off as: a y c x d.

What you can tell from this, is it doesn't matter what order we apply those changes. We always end up with the same result. It's commutative.  That's an example of a CRDT.

And it's needed to get consistent editing with the WOOT algorithm.

Now: how hard is this to implement? Well, it's less than 200 lines of regular Scala code.

To get this in the browser, I don't want to implement that again in JavaScript.  Not because it's particularly hard (I've done it, it's not), but because...


## you then have two problems.  

Two code bases to maintain, which have to be in sync, with two sets of tests.  That makes things harder to change, not easier.

You can maybe see why I like Scala.js -- it steps around those double-implementation, code duplication, problems.

## Signatures

Let's lots of aspects of the application we could look at, but I want to focus on the way two parts pan out.

If I tap a key in my editor, I need to do a local insert into my copy of the WOOT model.
I start with a WString, which is my document of WChars, and I insert a character at a particular position. This gives me back a document that contains the update, and a WChar which is basically what I need to send around the network.

That's the 5-element data structure.

If I receive one of these WChars from a remote source, I ingest that, which gives me a new document with the change included.


----

# PART 3

Those are the key signatures we need to expose. So how do we go about doing that?

## Diagram

Remember this diagram from earlier?  I want to look at this slice and what's in there.

And what we have splits between Browser and the JVM.  At the top we have the Ace editor.  And that's JavaScript, and as you can see we have a mix of technologies all the way down.

The WOOT algorithm itself is going is Scala.js... I mean, at this point it's will have been turned into JavaScript, but conceptually we should think of it a something separate.

We'll be exchanging messages over a websocket, and that'll be handled in JavaScript too.  That's also code we already have.

The message format will be JSON, and that will be implemented using a Scala.js library so I don't have to repeat myself there.

And the server side will be Scala, and it will consume JSON messages and apply them to its own copy of the WOOT document.

Working with a plain WOOT algorithm in the browser is a bit involved. I'm going to put a Scala.js wrapper around that code... because I want to expose to the JavaScript users a simple API, and not have to describe the IDs and the next and previous parts and all thtat stuff.

# Side by Side

Another way we can look at this stack is to separate the parts into the things that will just run on the client ... on the left. That run only on the server, on the right. And in the middle the parts, that's the code we are going to share.

That gives the structure of projects we need for SBT. One project for the client, one for the server, and then there's the shared code.

## SBT

Scala.js has an SBT plugin. And it provides some new commands, and also hooks into existing commands you know.

For example: when we compile a project, the server project produces class files as normal.  The client side, because is configured for scala-js,  produces class files and these Scala.js Intermediate Representations.  These are an unlinked, binary representation of the JavaScript.

Setting this up won't be a problem for SBT gurus. For the rest of us, it's a bit of a struggle, so that's something to be aware of. There are examples, on or linked from, the scala-js web site. And I'll put out a link to this project too, later today. It's maybe a bit daunting, but definitely achievable. So don't give up at this step.

But you'll note there's no JavaScript as such yet.

Producing that is the job of fastOptJs.  It produces JavaScript source, using the intermediate representation to only include what it needs to include. What can actually be reached from your code.

That will be something like 1.5 meg of JavaScrit. Which is fine for local development, and you'll probably sit in SBT running ~fastOptJs most of time.

A further command, fullOptJs, gets the JavaScript down to a few hundred k.

And these JS files you can include, using a script tag, just like you would any other JS file. There's no additional run time includes or anything like that. It's a self-contained piece of JavaScript.

# Testing

While we'll on the subject of the build, you can also depend on third party libraries. These libraries include the intermediate representation that feed into the scala-js compiler.  And the syntax you'll usually see is this tripple percent.

Here I'm also depending on ScalaCheck -- the properties-based testing framework.

You'll be familiar with %% as a way to allow dependencies to vary based on the Scala version you're building with.  %%% adds to that by configuring the dependency for the version of scala-js you're working with, but also makes sure the library used is the right one for a JVM project or a JavaScript project.

This mean you write a single test suite, and then run it in either the Scala project.

## Test command

... or the JavaScript client project.  And in JS the test will execute either with node, or rhino or phantomjs depending on what you configure.

This is especially good for shared code: not only do I implement the algorithm once, I can implement the tests once.

## Implementation

Let's look at our WOOT implementation.

This is the diagram from earlier, for a WChar, a WOOT character.

The implementation might be just case class, in which case it could look like this.

What's important here is that we've marked the class with @JSExport.  Remember earlier, I said  jsFastOpt removes code that's not referenced? Well, you need some starting points.  And you mark those starting points by exporting.

When we compile this code, and fastOptJs, we get a JS file out.  To use it...

...we include it like any other js file, and we can call the JavaScript versions of the code.

So that's OK. That works nicely.  But it's not what I do.

## But...

So first... working in terms of this case class is a bit complex.  We can be kinder to our JavaScript developers, and expose a better interface.

But more importantly, I've used a byte here rather than a char.  In the real code, I do use a char, but trying to expose that directly is a bit of a gotcha.

## Semantics

The semantics of Scala.js are the semantics of Scala. That's the principle. Which is a wonderful thing.

## Differences

There are some differences between Scala code and Scala-js code.

For example, there is no Char data type in JavaScript.Or Long. You have to decide how you want to deal with that, because it's a compile time error to try to expose a Char.

Also: there are differences in the way toString work in some places. It's not something that's impacted me, but toString on a floating point number produces different strings on JavaScript and Scala.

(Sébastien will be talking more about the semantics in the next session).

## Wrapper

We can improve on what we have with a short wrapper around the code.

Here we're exporting a class and in the class exporting two methods. These are the methods we'll call from JavaScript.

And note this code has a bit of state, which is the WOOT document.  So Scala.Js is going to realize we need that code, and make sure the WOOT algorithm is compiled to JavaScript and included in the JS file.

Let's look at the insert method first.  I'm avoid the char problem by simple accepting a String from JavaScript. But note that it returns some JSON.  How do we get that?

## Operation

That's produced by a Scala.js library called micro pickle.

What we send as JSON is really this Operation type.  There's a bit more to it than I'm showing here, but it's more or less an indication of whether you're inserting or deleting a character, and what character it is.

And what uPickle does is give us a nice API for turning that into JSON.

To produce JSON, we hand it a value.

To consume JSON, we try to read the JSON, and that will reconstruct a type for us. Which we can use somehow.

And this library is published for scala-js and the JVM, so we can use the same code in both places. Which means we have the same serialization in both places.

The default format is reasonable: we have a type, and then the fields.  So we can see this is an insert operation, and the character has some ID, and it's a * being inserted between the beginning and the end of the document. So this is probably the first character going into the document.

Libraries like this are important (there are a few you can choose from, include Argonaught)... and they are important because they make it easier to keep formats in sync.

Which makes it harder for us to screw up our application.

## Fill out implementation

So we can fill out the implementation of insert...

...which is to do the local insert on the WOOT model, update our state, and serialize the change to JSON.

This is all wired up in regular old JavaScript: a event on the ACE editor, calls this insert, and the result is handled, again in JavaScript, out to websocket.  You could do it all in Scala.js, but my focus is on re-use of what we already have, and adding Scala.js in gradually where it gives us the most benefit.

So a message goes out over a websocket. What happens when my browser receives an update?
That's handled by this ingest method. What I want to focus on with this method that it's Unit. It's a side-effect. We need to be able to update the editor somehow.

We have received an update from Alice or whoever, and we need to update the Ace editor state.

## Effecting

There are a bunch of ways to effect the DOM.  One is to use the scalajs-dom library.  This supplies a typed interface to the W3C DOM we all know and love. For example, we could go lookup an element by ID. And do something with that.

That's available, but that's not what I want to use.

Another option is to use, what I think of as a kind of emergency escape hatch in scala-js.
We can use Scala's Dynamic capabilities to make any sequence of calls we like, and scala-js will output them in JavaScript.

So here this code is the ACE editor API calls. This modifying the current document. And that will work, but you can see it's pretty dangerous.  If I mistype the name of one of those calls, it'll compile, but fail in the JavaScript runtime.

But I think it's important that this exists: it suggests you can, if you really have to, go do anything you want from Scala.js.   You're unlikely to get "stuck" where you simply can't do something with Scala.js

But I didn't use the dynamic route either.

# Functions

The route I ended up taking was to define a function in JavaScript that would do the updating.

The editor is defined in JavaScript, so along side that, I'm defining a function that will update it.  This seems reasonable. It will separate our WOOT client from the details of which editor we're using.

Doesn't particularly matter what this function is doing... it has to convert between document co-ordinate systems, and then call the right ACE editor API.  Uninteresting.  If I wanted to switch editors, I'd change that function.

What is interesting is that we can pass this JavaScript function into our scala-js class.

## How?

This is our Client code, and we can now fill in this part of accepting a JavaScript function and calling it.

The argument we pass in here is a type of js.FunctionN ... two argument function.  It expects a string and a boolean. And that matches up to what we're passing in from the JavaScript side.

We're storing that function as f.

So to ingest a remote change, we parse the JSON and apply the operation we find.

As usual: we call the WOOT algorithm and update state. But now we can match on the operation we decoded, and call the function f, with the character in the WChar, and a flag to signify insert or delete.

And that gives us the means to: expose our typed, compiled, scala-js to JavaScript, and write code that will call back to JavaScript we are passed.  Which is a great piece of technology to have available.

## The core WOOT algorithm itself...

It's OK, you don't need to read this... there's nothing Scala.js specific in it at all. It uses Vectors, tuples, pattern matching, traits, case classes, objects, tail recussion, maths libraries, and Chars (which it encodes, because it doesn't have to offer a public JavaScript interface)...there's nothing I had to do to make this work with Scala.js

# Recap

Let's just recap on what we've looked at.

We've seen we can share code, and separate out client and server in a build.

We can write Scala code and have the compiler watch over us.

We can represent and call JavaScript functions. JavaScript can call the values we export.

And if we really need to we can escape out to dynamic land of anything goes, using unchecked JavaScript calls.

We can do all of this using libraries, like uPickle, or scalacheck or scalaz or shapeless....

## CODE

The code for this is on github, and there will be a link at the end.

Clone the project, run the server, it'll do the jsFastOpt and then you can edit away in a browser.

I'm impressed with Scala.js, it's a level of maturity that's ready to use, you don't have to buy into any particular framework, it has a nice fast runtime.... but what are the real benefits for us.

## Benefits

I've asked myself what the benefits of all this are? Well, you get IDE support.  That scalajs-dom library I mentioned... there's get code completion on the DOM.  You get to write in a single language.  Neither of those two are important to me.  Being able to write code and run it in JS and the JVM... that still feels like magic.

But what I tell myself is the real benefit is...

Having the type system there. Capturing errors, refactoring safely, all of those benefits are wonderful.

But also being able to solve problems using the kinds of structures you want to. That's really important.

## Change

And that make it easier to maintain and evolve a project.

## Two Ideas

If you want to give it a go, remember that you can gradually add scala-js to a project. And there may be data structures or algorithms in you project that a really well suited to scala-js.

Thank you.
