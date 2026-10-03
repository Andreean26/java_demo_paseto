/**
 * CoachMark - Interactive Guided Tour Module
 * Modern, dependency-free guided tour / spotlight onboarding.
 */
(function (global) {
  'use strict';

  class CoachMarkManager {
    constructor() {
      this.steps = [];
      this.currentIndex = -1;
      this.overlay = null;
      this.spotlight = null;
      this.card = null;
      this.boundKeyHandler = this.onKeyDown.bind(this);
      this.boundResizeHandler = this.updatePosition.bind(this);
    }

    initElements() {
      if (this.overlay) return;

      this.overlay = document.createElement('div');
      this.overlay.className = 'coachmark-overlay';

      this.spotlight = document.createElement('div');
      this.spotlight.className = 'coachmark-spotlight';

      this.card = document.createElement('div');
      this.card.className = 'coachmark-card';
      this.card.setAttribute('role', 'dialog');
      this.card.setAttribute('aria-modal', 'true');

      this.overlay.appendChild(this.spotlight);
      this.overlay.appendChild(this.card);
      document.body.appendChild(this.overlay);

      this.overlay.addEventListener('click', (e) => {
        if (e.target === this.overlay) {
          this.close();
        }
      });
    }

    start(steps, options = {}) {
      if (!steps || !steps.length) return;
      this.steps = steps.filter((s) => !!document.querySelector(s.element));
      if (!this.steps.length) return;

      this.options = options;
      this.initElements();
      this.currentIndex = 0;

      window.addEventListener('keydown', this.boundKeyHandler);
      window.addEventListener('resize', this.boundResizeHandler);
      window.addEventListener('scroll', this.boundResizeHandler, { passive: true });

      this.overlay.classList.add('active');
      document.body.classList.add('coachmark-active');
      this.showStep(this.currentIndex);
    }

    showStep(index) {
      if (index < 0 || index >= this.steps.length) {
        this.close();
        return;
      }

      this.currentIndex = index;
      const step = this.steps[index];
      const target = document.querySelector(step.element);

      if (!target) {
        this.next();
        return;
      }

      // Scroll target into view with nearest block to avoid jarring jumps
      target.scrollIntoView({
        behavior: 'smooth',
        block: 'nearest',
        inline: 'nearest'
      });
      // Render tooltip card content
      const isFirst = index === 0;
      const isLast = index === this.steps.length - 1;
      const stepTotal = this.steps.length;

      this.card.innerHTML = `
        <div class="coachmark-header">
          <span class="coachmark-badge">Langkah ${index + 1} dari ${stepTotal}</span>
          <button class="coachmark-close" type="button" aria-label="Tutup panduan">&times;</button>
        </div>
        <div class="coachmark-content">
          <h3 class="coachmark-title">${step.title || ''}</h3>
          <p class="coachmark-desc">${step.description || ''}</p>
        </div>
        <div class="coachmark-footer">
          <button class="coachmark-skip-btn" type="button">Lewati</button>
          <div class="coachmark-actions">
            ${!isFirst ? '<button class="coachmark-prev-btn" type="button">&larr; Kembali</button>' : ''}
            <button class="coachmark-next-btn primary" type="button">${isLast ? 'Selesai &#10003;' : 'Lanjut &rarr;'}</button>
          </div>
        </div>
      `;

      // Event listeners
      this.card.querySelector('.coachmark-close').addEventListener('click', () => this.close());
      this.card.querySelector('.coachmark-skip-btn').addEventListener('click', () => this.close());
      const prevBtn = this.card.querySelector('.coachmark-prev-btn');
      if (prevBtn) prevBtn.addEventListener('click', () => this.prev());
      this.card.querySelector('.coachmark-next-btn').addEventListener('click', () => this.next());

      // Multiple frames to guarantee correct placement during and after smooth scroll
      requestAnimationFrame(() => this.updatePosition());
      setTimeout(() => this.updatePosition(), 100);
      setTimeout(() => this.updatePosition(), 350);
    }

    updatePosition() {
      if (this.currentIndex < 0 || !this.steps[this.currentIndex]) return;
      const target = document.querySelector(this.steps[this.currentIndex].element);
      if (!target || !this.spotlight || !this.card) return;

      const rect = target.getBoundingClientRect();
      const padding = 8;

      // Position spotlight box
      this.spotlight.style.top = `${rect.top - padding}px`;
      this.spotlight.style.left = `${rect.left - padding}px`;
      this.spotlight.style.width = `${rect.width + padding * 2}px`;
      this.spotlight.style.height = `${rect.height + padding * 2}px`;

      // Position tooltip card relative to target using 4-way smart placement
      const cardRect = this.card.getBoundingClientRect();
      const margin = 16;
      const viewW = window.innerWidth;
      const viewH = window.innerHeight;

      const spaceBelow = viewH - rect.bottom - margin;
      const spaceAbove = rect.top - margin;
      const spaceLeft = rect.left - margin;
      const spaceRight = viewW - rect.right - margin;

      let top = margin;
      let left = margin;

      if (spaceBelow >= cardRect.height) {
        // 1. Fits below target
        top = rect.bottom + margin;
        left = rect.left;
      } else if (spaceAbove >= cardRect.height) {
        // 2. Fits above target
        top = rect.top - cardRect.height - margin;
        left = rect.left;
      } else if (spaceLeft >= cardRect.width) {
        // 3. Target is tall, fits on LEFT side
        left = rect.left - cardRect.width - margin;
        top = Math.max(margin, rect.top);
      } else if (spaceRight >= cardRect.width) {
        // 4. Target is tall, fits on RIGHT side
        left = rect.right + margin;
        top = Math.max(margin, rect.top);
      } else {
        // 5. Fallback: viewport is constrained, place where there is more vertical space
        if (spaceBelow >= spaceAbove) {
          top = rect.bottom + margin;
        } else {
          top = rect.top - cardRect.height - margin;
        }
        left = rect.left;
      }

      // ALWAYS clamp within viewport margins so tooltip is NEVER cut off
      const maxTop = Math.max(margin, viewH - cardRect.height - margin);
      const maxLeft = Math.max(margin, viewW - cardRect.width - margin);

      this.card.style.top = `${Math.max(margin, Math.min(maxTop, top))}px`;
      this.card.style.left = `${Math.max(margin, Math.min(maxLeft, left))}px`;
    }

    next() {
      if (this.currentIndex < this.steps.length - 1) {
        this.showStep(this.currentIndex + 1);
      } else {
        this.close();
      }
    }

    prev() {
      if (this.currentIndex > 0) {
        this.showStep(this.currentIndex - 1);
      }
    }

    close() {
      this.currentIndex = -1;
      if (this.overlay) {
        this.overlay.classList.remove('active');
      }
      document.body.classList.remove('coachmark-active');
      window.removeEventListener('keydown', this.boundKeyHandler);
      window.removeEventListener('resize', this.boundResizeHandler);
      window.removeEventListener('scroll', this.boundResizeHandler);
      if (this.options && typeof this.options.onComplete === 'function') {
        this.options.onComplete();
      }
    }

    onKeyDown(e) {
      if (e.key === 'Escape') {
        this.close();
      } else if (e.key === 'ArrowRight') {
        this.next();
      } else if (e.key === 'ArrowLeft') {
        this.prev();
      }
    }
  }

  global.CoachMark = new CoachMarkManager();
})(window);
