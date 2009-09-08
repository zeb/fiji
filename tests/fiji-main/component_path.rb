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

cdialog = Java::JavaAwt::Dialog.new(nil, "ComponentPathTest")
panel = Panel.new
panel.add(Label.new('Hello'))
panel.add(Button.new('Hello'))
panel.add(Button.new('Hello'))
panel.add(TextField.new('Bla'))
panel.add(Label.new('Hello'))
panel.add(Button.new('Hello'))
cdialog.add(panel)

cdialog.pack
cdialog.setVisible(true)

cdialog.getAllComponents.each do |co|

  path = Main.getPath(co)
  result = Main.getComponent(path) == co

  puts "co = #{co}"
  puts "  path = #{path}"
  puts "  getComponent(getPath(co)) == co: #{color(result)}"
  puts
end

cdialog.setVisible(false)
Java::JavaLang::System.exit(0)
