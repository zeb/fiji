#!/usr/bin/env ruby
#

# TODO
#   replace obj.getFoo with obj.foo

require 'java'

include_class 'fiji.Main'

include_class 'ij.IJ'
include_class 'ij.ImageJ'
include_class 'ij.WindowManager'

include_class 'java.awt.Button'
include_class 'java.awt.Container'
include_class 'java.awt.Dialog'
include_class 'java.awt.Frame'
include_class 'java.awt.MenuBar'
include_class 'java.awt.Toolkit'

include_class 'java.awt.event.AWTEventListener'
include_class 'java.awt.event.ActionEvent'
include_class 'java.awt.event.ContainerEvent'
include_class 'java.awt.event.FocusEvent'
include_class 'java.awt.event.HierarchyEvent'
include_class 'java.awt.event.MouseEvent'

# Let's get each, find, select & friends for low
module Java::JavaAwt
  class MenuBar
    include Enumerable

    def each(&block)
      (0..menu_count-1).each do |x|
	yield menu(x)
      end
    end
  end


  class Menu
    include Enumerable

    def each(&block)
      (0..item_count-1).each do |x|
	yield item(x)
      end
    end
  end
end

module TestLib
  @currentWindow = nil

  def self.startIJ
    Main.premain
    @currentWindow = ImageJ.new
    @currentWindow.exitWhenQuitting(true)
    Main.postmain
  end

  def self.catchIJErrors(&block)
    begin
      IJ.redirectErrorMessage
      return yield
    rescue
      logWindow = WindowManager.getFrame('Log')
      if logWindow
	error_message = logWindow.getTextPanel.getText
	logWindow.close
	return error_message
      end
    end
  end

  def self.test(&block)
    if catchIJErrors(&block)
      print "Failed: " + block.to_s
      exit 1
    end
  end

  def self.waitForWindow(title)
    @currentWindow = Main.waitForWindow(title)
    cf = Frame.getFrames.find { |frame| frame.getTitle == title }
    return @currentWindow
  end

  def self.getMenuEntry(menuBar, path)
    menubar ||= @currentWindow.menu_bar
    path = path.split('>') if path.is_a? String

    menu = menubar.find { |x| x.label == path[0] }

    path[1..-1].each do |label|
      menu = menu.find { |x| x.label == label }
      break unless menu
    end

    return menu
  end

  def self.dispatchActionEvent(component)
    event = ActionEvent.new(component, ActionEvent::ACTION_PERFORMED,
			    component.label, MouseEvent::BUTTON1)
    component.dispatchEvent(event)
  end

  def self.clickMenuItem(path)
    menuEntry = getMenuEntry(nil, path)
    dispatchActionEvent(menuEntry)
  end

  def self.getButton(container, label)
    container ||= @currentWindow
    container.components.each do |co|
      if co.is_a? Button and co.label == label
	return co
      elsif co.is_a? Container and r = getButton(co, label)
	return r
      end
    end

    return nil
  end

  def self.clickButton(label)
     button = getButton(nil, label)
     dispatchActionEvent(button)
  end

  def self.quitIJ
    IJ.getInstance.quit
    @currentWindow = nil
  end

=begin To Be Tested
  class OutputThread < Thread
    def initialize(input, output)
      @buffer = zeroes(65536, 'b')
      @input = input
      @output = output
    end

    def run()
      loop do
	count = @input.read(@buffer)
	return if count < 0
	@output.write(@buffer, 0, count)
      end
    end
  end

  def self.launchFiji(args, workingDir = nil)
  end
=end
end
