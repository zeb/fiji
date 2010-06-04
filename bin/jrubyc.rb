#!/bin/sh
foo=# exec "$(dirname "$0")"/../fiji -Djruby.home="$(dirname "$0")"/../jruby --jruby -- "$0" "$@"

require 'jruby/compiler'

status =  JRuby::Compiler::compile_argv(ARGV)

if (status != 0)
  puts "Compilation FAILED: #{status} error(s) encountered"
  exit status
end
