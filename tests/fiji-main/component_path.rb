#!/usr/bin/env fiji
#

require 'java'
require '../testlib' # Container.getAllComponents

include_class 'fiji.Main'
include_class 'java.awt.Button'
include_class 'java.awt.Label'
include_class 'java.awt.Panel'
include_class 'java.awt.TextField'

def color(bool)
  color = bool ? 32 : 31

  return "\x1b[#{color};01m#{bool}\x1b[m"
end

Main.premain

pathes = Hash.new

cdialog   = Java::JavaAwt::Dialog.new(nil, "ComponentPathTest")
panel     = Panel.new
label1    = Label.new('Hello')
button1   = Button.new('Hello')
button2   = Button.new('Hello')
textfield = TextField.new('Bla')
label2    = Label.new('Hello')
button3   = Button.new('Hello')

pathes[panel] =
  'ComponentPathTest>class java.awt.Panel[0]'
pathes[label1] =
  'ComponentPathTest>class java.awt.Panel[0]>class java.awt.Label{Hello}'
pathes[button1] = 
  'ComponentPathTest>class java.awt.Panel[0]>class java.awt.Button{Hello}'
pathes[button2] = 
  'ComponentPathTest>class java.awt.Panel[0]>class java.awt.Button{Hello}[2]'
pathes[textfield] =
  'ComponentPathTest>class java.awt.Panel[0]>class java.awt.TextField{Hello}'
pathes[label2] =
  'ComponentPathTest>class java.awt.Panel[0]>class java.awt.Label[1]'
pathes[button3] = 
  'ComponentPathTest>class java.awt.Panel[0]>class java.awt.Button[2]'

panel.add(label1)
panel.add(button1)
panel.add(button2)
panel.add(textfield)
panel.add(label2)
panel.add(button3)
cdialog.add(panel)

cdialog.pack
cdialog.setVisible(true)

cdialog.getAllComponents.each do |co|

  path = Main.getPath(co)
  result1 = pathes[co] == path
  result2 = Main.getComponent(path) == co

  puts "co = #{co}"
  puts "  getPath(co) = #{path}"
  puts "  getPath(co) returns expected result: #{color(result1)}"
  puts "  getComponent(getPath(co)) == co:     #{color(result2)}"
  puts
end

cdialog.setVisible(false)
Java::JavaLang::System.exit(0)
