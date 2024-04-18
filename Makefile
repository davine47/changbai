test:
	mill emulator[sandbox.Hello,sandbox.HelloConfig].mfccompiler.compile

hello:
	mill -i changbai.spinal.runMain changbai.sayHello

test-hello:
	mill -i changbai.spinal.test.runMain changbai.test.genTestChangbai

changbai:
	mill -i changbai.spinal.runMain changbai.genChangbai

idea:
	mill mill.idea.GenIdea/idea