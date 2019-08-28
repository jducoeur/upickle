## Shaded fork of uPickle

Many of our libraries need to work with JSON. However, we have found (through
long painful experience) that letting them use Play-Json puts us into a
particular form of Dependency Hell: the libraries wind up dependent on
a *specific version* of Play-Json, which means that upgrading Play requires
upgrading much of our ecosystem, which is a hassle. We want to decouple
our libraries from Play as much as possible, to reduce this friction.

So we are encouraging libraries to make use of uPickle instead: it's
popular, well-supported and fast.

However, if we allowed libraries to simply pick random versions of
uPickle, we'd be right back where we were with Play-Json: if different
libraries used different versions, we could wind up with evictions and
runtime collisions, since uPickle isn't shaded.

So this is a shaded fork of uPickle. It is hard-shaded (instead of using
sbt-assembly or something like that) because uPickle includes macros with
hard-coded paths, so automatic shading isn't likely to work correctly.

### Status

As of this writing, this fork wanders a little from the standard
uPickle in how it builds, because for the time being we still require
Scala 2.11, as well as 2.13. So this is forked from uPickle SHA 
`feaeb8b` (May 19, 2019), just before Scala 2.11 support was dropped in
the main fork.

This is currently incomplete: not all of the many possible permutations
of versions and targets are building. This is largely because the
versions of many components are different for 2.11 and 2.13, and we
haven't yet tracked down all of the puzzle pieces. For now, we
are focusing on getting the key artifacts that we care about
(uPickle's core and the Play-Json adapter) working on the JVM
for 2.11 and 2.13. Contributions to flesh out the rest of this
are welcome.

Due to these artifact conflicts between 2.11 and 2.13, the build.sc
file is currently kind of horrible, and could use a good thorough
refactoring.

### Building uPickle

uPickle is based on Mill, not sbt. (There is an sbt file, but it's just
for the documentation.) In order to build this, you will need to install
Mill:
```
brew install mill
```

#### Compile

To build the entire system, say:
```
mill __.compile
```
In Mill, a single underscore `_` is the wildcard, and a double underscore
`__` is a recursive wildcard that drills into sub-modules. So the above
formula means "find all modules and submodules, and compile them".

#### Testing

Similarly, to run all unit tests:
```
mill __.test
```

#### Packaging

To create the JAR files:
```
mill __.jar
```

(There's also a `__.assembly` task, which I believe creates fatjars,
but I can't see why we would care in this case.)

#### When Things Silently Fail

Mill has one iffy characteristic: when something is broken in the
`build.sc` file, it will often fail silently. No errors or anything;
just nothing happens.

When this occurs, the `resolve` command tends to be a lifesaver. This
steps back and just shows what tasks *would* be run by the given
command, and it does generally show the errors.

So for example, when compile seems to be dead in the water, say:
```
mill resolve __.compile
```
and it will generally show you what's broken.

### Original Readme

uPickle: a simple Scala JSON and Binary (MessagePack) serialization library

- [Documentation](https://lihaoyi.github.io/upickle)

If you use uPickle and like it, please support it by donating to our Patreon:

- [https://www.patreon.com/lihaoyi](https://www.patreon.com/lihaoyi)

[![Build Status](https://travis-ci.org/lihaoyi/upickle.svg)](https://travis-ci.org/lihaoyi/upickle)


[![Join the chat at https://gitter.im/lihaoyi/upickle](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/lihaoyi/upickle?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
