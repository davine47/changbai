# changbai
first commit
light-ly joined

# start simulation

## on macos
Setup a virtual python environment
```shell
$ cd changbai/cocotb_example
$ python3 -m venv ./venv
$ source ./venv/bin/activate
$ python3 -m pip install cocotb
$ make
```
Or use pyenv to manage global python libs
```shell
$ brew install pyenv
```
Add path to .bash_profile
```shell
$ eval “$(pyenv init -)”
```
```shell
$ source .bash_profile
```
```shell
$ pyenv install 3.11.2
$ pyenv rehash 
$ pyenv versions 
$ pyenv global 3.4.3
$ pyenv versions
$ python
$ pip3 install cocotb #1.8.1
```
then setup pycharm according global python3 path
```shell
$ which python3
```
We recommand to use python versions which less than 3.12 because of distutils module dependence.
