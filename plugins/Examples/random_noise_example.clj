; Albert Cardona 20080427 at MPI-CBG Dresden FIJI hackathon.

(import '(ij.process ByteProcessor)
	'(ij ImagePlus)
	'(java.awt.image IndexColorModel)
	)


(defn int-to-ubyte
  "Converts the integer x (arg1) in the range [0, 255] to a byte. Note that bytes in clojure and java are
regrettably always signed."
  [ x ]
  {:pre [(<= 0 x 255)]}
  (byte (if (< x 128) x (- x 256))))


; A closure over a new instance of Random,
; used in an inner function that generates random bytes
(let [rand (new java.util.Random)]
  (defn rand-byte []
	(int-to-ubyte (.nextInt rand 256))))

; Create a new image and set each pixel to a random byte
(let [bp (new ByteProcessor 512 512)
      pix (. bp (getPixels))]
  (dotimes [i (count pix)]
    (aset pix i (rand-byte)))
  (.show (ImagePlus. "random" bp)))

; Returns a new grayscale LUT
(defn make-grayscale-lut []
      (defn make-channel []
	    (let [channel (make-array Byte/TYPE 256)]
	      (dotimes [i 256]
			(aset channel i (int-to-ubyte i)))
	      channel))
      (IndexColorModel. 8 256 (make-channel) (make-channel) (make-channel)))

; Create a second image directly from a byte array
(let [pix (make-array Byte/TYPE (* 512 512))]
  (dotimes [i (count pix)]
    (aset pix i (rand-byte)))
  (.show (ImagePlus. "random 2" (ByteProcessor. 512 512 pix (make-grayscale-lut)))))
