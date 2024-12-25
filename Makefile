test:
	mill emulator[sandbox.Hello,sandbox.HelloConfig,DiplomacyGenerator].mfccompiler.compile
test1:
	mill emulator[examples.Adder,sandbox.HelloConfig,SimpleGenerator].mfccompiler.compile

hello:
	mill -i changbaiV1.spinal.runMain changbaiV1.sayHello

idea:
	mill mill.idea.GenIdea/idea

clean:
	rm -rf out/ *.lst changbaiTest/