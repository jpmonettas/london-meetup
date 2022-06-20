(require 'face-remap)
(require 'org)
(require 'org-element)


(defconst slide-top-marker ";;------------")
(defvar slide-margin-top 10)
(defconst slide-height 20)
(defconst slide-width 100)
(defconst left-margin 39)
(defconst code-slide-marker ?:)
(defconst text-slide-marker ?.)

(defun slides/make-slide-top (title is-code)
  (interactive (list (read-string "Title :")
					 (string-equal "yes" (read-answer "Is it a code slide? "
													  '(("yes" ?y "It is a code slide")
														("no" ?n "It is a text slide"))))))
  
  (let* ((title-width (length title))
		 (half-bars-with (/ (- slide-width title-width 2) 2)))

    (insert (format ";;%s" (make-string half-bars-with ?-)))
	(insert (format "( %s )" title))
    (insert (make-string half-bars-with ?-))
	(insert-char (if is-code code-slide-marker text-slide-marker))))

(defun slides/make-slide-bottom ()
  (interactive)
  
  (dotimes (_ slide-height)
	(next-line))
  
  (insert (format ";%s" (make-string (+ 4 slide-width) ?-))))

(defun slides/set-current-slide-lock-mode ()
  (if (= (char-before) code-slide-marker)
	  (font-lock-mode 1)
	(font-lock-mode -1)))

(defun slides/next-slide ()
  (interactive)
  (search-forward slide-top-marker)
  (end-of-line)
  (recenter-top-bottom slide-margin-top)
  (slides/set-current-slide-lock-mode))

(defun slides/prev-slide ()
  (interactive)
  (beginning-of-line)
  (search-backward slide-top-marker)
  (end-of-line)
  (recenter-top-bottom slide-margin-top)
  (slides/set-current-slide-lock-mode))

(defun slides/add-margin ()
  (interactive)  
  (save-excursion
	(beginning-of-buffer)
	(search-forward slide-top-marker)
	(previous-line)
	(replace-regexp "^" (make-string left-margin ?\s))))

(defun slides/remove-margin ()
  (interactive)  
  (save-excursion
	(beginning-of-buffer)
	(search-forward slide-top-marker)
	(previous-line)
	(replace-regexp (format "^%s"(make-string left-margin ?\s)) "")))

(defvar is-left-margin-set nil)

(defun slides/toggle-left-margin ()
  (interactive)
  (if is-left-margin-set
	  (progn
		(setq slide-margin-top 10)
		(slides/remove-margin)
		(setq text-scale-mode-amount 2)
		(text-scale-mode 1)
		(setq is-left-margin-set nil))
	
	(progn
	  (setq slide-margin-top 7)
	  (slides/add-margin)
	  (setq text-scale-mode-amount 3)
	  (text-scale-mode 1)
	  (setq is-left-margin-set 't))))

(define-minor-mode slides-mode
  "Minor mode for working with basic text slides"
  :lighter " foo"
  :keymap (let ((map (make-sparse-keymap)))
            (define-key map (kbd "<home>") 'slides/prev-slide)
            (define-key map (kbd "<end>")  'slides/next-slide)
            (define-key map (kbd "<prior>") 'slides/toggle-left-margin)
            map)
  (progn
	(setq text-scale-mode-amount 2)
	(text-scale-mode 1)
	(org-display-inline-images)
	(load-theme 'doom-nord-light)))

