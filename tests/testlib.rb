#!/usr/bin/env ruby
#

# TODO
#   replace obj.getFoo with obj.foo

require 'java'

include_class 'fiji.Main'

include_class 'ij.IJ'
#include_class 'ij.ImageJ'
#include_class 'ij.WindowManager'

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

include_class 'java.lang.Runtime'
include_class 'java.lang.System'

# Let's get each, find, select & friends for low
module Java::JavaAwt
  class Container
    def getAllComponents(type = Java::JavaAWT::Component)
      children = Array.new
      self.getComponents.each do |child|
	children.push(child) if child.is_a?(type)

	if child.respond_to?(:getAllComponents) and r = child.getAllComponents(type)
	  children.push(*r)
	end
      end

      return children
    end
  end

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
    @currentWindow = Java::Ij::ImageJ.new
    @currentWindow.exitWhenQuitting(true)
    Main.postmain
  end

  def self.catchIJErrors(&block)
    begin
      IJ.redirectErrorMessage
      return yield
    rescue
      logWindow = Java::Ij::WindowManager.getFrame('Log')
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
    return @currentWindow
  end

  def self.getMenuEntry(menuBar, path)
    menubar ||= @currentWindow.menu_bar
    path = path.split('>') if path.is_a? String

    unless menu = menubar.find { |x| x.label == path[0] }
      return nil
    end

    path[1..-1].each do |label|
      menu = menu.find { |x| x.label == label }
      break unless menu
    end

    return menu
  end

  def self.getComponentByPath(path, container = nil)
    container ||= @currentWindow

    return if path.nil?

    if path.is_a? String and path =~ />/
      path = path.split('>')
    end

    if path.is_a? String
      match = path.match(/^([A-Z][_[:alnum:]]*(::[A-Z][_[:alnum:]]*)*)(\[([0-9]+)\])?(\{(.+)\})?$/)

      return nil if match.nil?

      name = match[1]
      nth  = match[4]
      text = match[6]


      candidates = container.getComponents.select { |x| x.is_a? eval(name) }

      unless nth.nil?
	candidate = candidates[nth.to_i]
	candidates = candidate.nil? ? [] : [candidate]
      end

      unless text.nil?
	candidates = candidates.select do |elem|
	  if elem.respond_to? :getLabel
	    elem.getLabel == text
	  elsif elem.respond_to? :getText
	    elem.getText == text
	  end
	end
      end

      return candidates.first
    end

    if path.is_a? Array
      path.each do |elem|
	container = getComponentByPath(elem, container)
	break unless container
      end

      return container
    end
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

=begin Nah, not yet
  class OutputThread < Java::JavaLang::Thread
    def initialize(input, output)
      @buffer = [].fill(0, 0, 65536)
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

  def self.launchFiji(args, working_dir = nil)
    $stderr.puts("self.launchFiji(#{args.inspect}, #{working_dir.inspect})")
    args = args.to_a
    args.insert(0, System.getProperty('fiji.executable'))
    begin
      $stderr.puts("#{args.join(" ").inspect}")
      process = Runtime.getRuntime.exec(args.join(" "), nil, working_dir)
      $stderr.puts("process = #{process.inspect}")
      OutputThread.new(process.getInputStream, System.out).start
      $stderr.puts("out")
      OutputThread.new(process.getErrorStream, System.err).start
      $stderr.puts("err")
      $stderr.puts("There we go...")
      return process.waitFor
    rescue => e
      $stderr.puts("exception: #{e.inspect}")
      return -1
    end
  end
=end
end
