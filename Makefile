test:
	mill emulator[sandbox.Hello,sandbox.HelloConfig].mfccompiler.compile

spinal-adder:
	mill -i changbai.spinal.runMain v1.genAdder

hello:
	mill -i changbai.spinal.runMain changbai.sayHello

test-hello:
	mill -i changbai.spinal.test.runMain changbai.test.genTestChangbai

changbai:
	mill -i changbai.spinal.runMain changbai.genChangbai

idea:
	mill mill.idea.GenIdea/idea