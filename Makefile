test:
	mill emulator[sandbox.Hello,sandbox.HelloConfig].mfccompiler.compile

hello:
	mill -i changbai.spinal.runMain changbai.sayHello

changbai:
	mill -i changbai.spinal.runMain changbai.genChangbai