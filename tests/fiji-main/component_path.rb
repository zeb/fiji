#!/usr/bin/env fiji
#

require 'java'
require '../testlib' # Container.getAllComponents

include_class 'fiji.Main'
include_class 'java.awt.Button'
include_class 'java.awt.Dialog'
include_class 'java.awt.Label'
include_class 'java.awt.Panel'
include_class 'java.awt.TextField'

def color(bool)
  color = bool ? 32 : 31

  return "\x1b[#{color};01m#{bool}\x1b[m"
end

def die(message)
  star = " \x1b[33;01m*\x1b[m"
  $stderr.puts star
  $stderr.puts "#{star} Failed test: #{message}"
  $stderr.puts star

  exit 1
end

def test(message, result)
  puts "#{message}: #{color(result)}"
  die(message) unless result
end

Main.premain

components = [
  [Panel.new, 'ComponentPathTest>class java.awt.Panel[0]'],
  [Label.new('Hello'), 'ComponentPathTest>class java.awt.Panel[0]>class java.awt.Label{Hello}'],
  [Button.new('Hello'), 'ComponentPathTest>class java.awt.Panel[0]>class java.awt.Button{Hello}'],
  [Button.new('Hello'), 'ComponentPathTest>class java.awt.Panel[0]>class java.awt.Button{Hello}[2]'],
  [TextField.new('Bla'), 'ComponentPathTest>class java.awt.Panel[0]>class java.awt.TextField{Hello}'],
  [Label.new('Hello'), 'ComponentPathTest>class java.awt.Panel[0]>class java.awt.Label[1]'],
  [Button.new('Hello'), 'ComponentPathTest>class java.awt.Panel[0]>class java.awt.Button[2]']
]

components[1..-1].each { |co,_| components[0][0].add(co) }

cdialog = Dialog.new(nil, 'ComponentPathTest')
cdialog.add(components[0][0])

cdialog.pack
cdialog.setVisible(true)

cdialog.getAllComponents.each do |co|
  expect = components.find { |c,l| c == co }

  path_of_component = Main.getPath(co)
  component_by_path = Main.getComponent(path_of_component)

  result1 = expect[1] == path_of_component
  result2 = component_by_path == co

  puts "co = #{co}"
  puts "  getPath(co) returns #{path_of_component}"
  test("  getPath(co) returns expected result", result1)
  test("  getComponent(getPath(co)) == co", result2)
  puts ""
end

cdialog.setVisible(false)
Java::JavaLang::System.exit(0)
