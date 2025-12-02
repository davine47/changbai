# changbai

# Environment (Dependencies) on macos Sequoia 15.6

```shell
$ export CHANGBAI_ROOT=$PWD
$ echo $CHANGBAI_ROOT
```

## pyenv, python3, cocotb

Use pyenv to manage global python libs
```shell
$ brew install pyenv
```

Add path to .zshrc
```shell
eval "$(pyenv init --path)"
eval "$(pyenv init -)"
```
```shell
$ source .zshrc
```
```shell
$ pyenv install 3.12.12 # less than this version
$ pyenv rehash 
$ pyenv versions
$ which python3 # confirm your python3 path
$ pip3 install cocotb
```
Or setup a virtual python environment in shell.
```shell
$ cd changbai/cocotb_example
$ python3 -m venv ./venv
$ source ./venv/bin/activate
$ python3 -m pip install cocotb
$ make
```
## verilator
```shell
$ git clone https://github.com/verilator/verilator
$ cd verilator
$ export VERILATOR_ROOT=$PWD
$ autoconf
$ ./configure

$ make -j8
```
Add local verilator path into .zshrc
```shell
export PATH="/Users/username/verilator/bin:$PATH"
```
```shell
$ source .zshrc
```

## gtkwave
https://gtkwave.github.io/gtkwave/install/mac.html
```shell
$ git clone https://github.com/gtkwave/gtkwave.git gtkwave
$ cd gtkwave
$ brew install desktop-file-utils shared-mime-info gobject-introspection gtk-mac-integration meson ninja pkg-config gtk+3 gtk4
# Use --prefix to specify the installation path
$ meson setup build --prefix=/opt
# Or install to default path (/usr/local):
# meson setup build
$ meson compile -C build # Start compile
$ sudo meson install -C build # Install gtkwave
$ gtkwave
```

# Run simulation with cocotb

## Run an example
```shell
$ cd changbai
$ make rtl
$ cd changbai/coco_tb/test
$ make
$ gtkwave dump.vcd
```

# Software stack
## riscv-opcodes
```shell
$ cd changbai/sw/riscv-opcodes
$ make EXTENSIONS='rv64_i rv64_m'
```
Or use scripts to update scala files
```shell
# spinalhdl v1
$ ./scripts/update_spinalhdl_v1_instructions.sh
```



