#!/usr/bin/env fiji
#

require 'java'
require '../testlib'

include_class 'fiji.Main'
include_class 'java.awt.Button'
include_class 'java.awt.Label'
include_class 'java.awt.Panel'
include_class 'java.awt.TextField'

class ComponentPathDialog < Java::JavaAwt::Dialog
  def initialize
    super(nil, "ComponentPathTest")

    @panel = Panel.new
    @panel.add(Label.new('Hello'))
    @panel.add(Button.new('Hello'))
    @panel.add(Button.new('Hello'))
    @panel.add(TextField.new('Bla'))
    @panel.add(Label.new('Hello'))
    @panel.add(Button.new('Hello'))

    self.add(@panel)
  end
end

if $0 == __FILE__
  cdialog = ComponentPathDialog.new
  cdialog.pack
  cdialog.setVisible(true)

  cdialog.getAllComponents.each do |co|
    path = Main.getPath(co)
    result = Main.getComponent(Main.getPath(co)) == co

    puts "co = #{co}"
    puts "  path = #{path}"
    puts "  getComponent(dialog, getPath(co)) == co: #{result}"
  end
end
