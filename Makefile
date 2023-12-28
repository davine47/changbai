test:
	mill emulator[sandbox.Hello,sandbox.HelloConfig].mfccompiler.compile

hello:
	mill -i changbai.runMain changbai.sayHello

changbai:
	mill -i changbai.runMain changbai.genChangbai