JAVAC := javac
JAVACC:= javacc

default	:	compiler

compiler :	select.jar

# options: c--create; f--name of jar file; e--entry point
select.jar	:
	$(JAVAC) -cp .:${SUPPORT} */*.java
	jar cfm select.jar compile.txt parser/*.class syntax/*.class main/*.class

clean	:
	-/bin/rm *~ */*~
	-/bin/rm */*.class

