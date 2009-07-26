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
include_class 'java.awt.Toolkit'

include_class 'java.awt.event.AWTEventListener'
include_class 'java.awt.event.ActionEvent'
include_class 'java.awt.event.ContainerEvent'
include_class 'java.awt.event.FocusEvent'
include_class 'java.awt.event.HierarchyEvent'
include_class 'java.awt.event.MouseEvent'

module TestLib
  @currentWindow = nil

  def self.startIJ
    Main.premain
    @currentFrame = ImageJ.new
    @currentFrame.exitWhenQuitting(true)
    Main.postmain
  end

  def catchIJErrors(&block)
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

  def test(&block)
    if catchIJErrors(&block)
      print "Failed: " + block.to_s
      exit 1
    end
  end

  def waitForWindow(title)
    @currentWindow = Main.waitForWindow(title)
    cf = Frame.getFrames.find { |frame| frame.getTitle == title }
    return @currentWindow
  end

  def getMenuEntry(menuBar, path)
    menuBar ||= @currentFrame.getMenuBar
    path = path.split('>') if path.is_a? String

    begin
      menu = nil
      menuBar.getMenuCount.times do |i|
	menu = menuBar.getMenu(i) if  path[0] == menuBar.getMenu(i).getLabel
      end

      path[1..-1].each do |label|
	entry = nil
	menu.getItemCount.times do |i|
	  entry = menu.getItem(i) if label == menu.getItem(i).getLabel
	  break
	end
	menu = entry
      end

      return menu
    rescue
      return nil
    end
  end

  def dispatchActionEvent(component)
    event = ActionEvent.new(component, ActionEvent::ACTION_PERFORMED,
			    component.getLabel, MouseEvent::BUTTON1)
    component.dispatchEvent(event)
  end

  def clickMenuItem(path)
    menuEntry = getMenuEntry(nil, path)
    dispatchActionEvent(menuEntry)
  end

  def getButton(container, label)
    container ||= @currentDialog
    container.getComponents.each do |co|
      if co.is_a? Container
	return result if result = getButton(co, label)
      elsif co.is_a? Button and co.getLabel == label
	return co
      end
    end

    return nil
  end

  def clickButton(label)
     button = getButton(nil, label)
     dispatchActionEvent(button)
  end

  def self.quitIJ
    IJ.getInstance.quit
    @currentFrame = nil
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
