import React, { useEffect } from 'react';

interface FullScreenModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  children: React.ReactNode;
}

/**
 * CRUCIAL RULE: 100dvh Full-Screen Modal with strict body scroll locking.
 * Strictly NO bottom sheets to prevent iOS Safari rubber-banding & viewport scrolling bugs.
 */
export const FullScreenModal: React.FC<FullScreenModalProps> = ({
  isOpen,
  onClose,
  title,
  children,
}) => {
  useEffect(() => {
    if (!isOpen) return;
    const scrollY = window.scrollY;
    const originalStyle = {
      overflow: document.body.style.overflow,
      position: document.body.style.position,
      top: document.body.style.top,
      width: document.body.style.width,
    };

    document.body.style.overflow = 'hidden';
    document.body.style.position = 'fixed';
    document.body.style.top = `-${scrollY}px`;
    document.body.style.width = '100%';

    return () => {
      document.body.style.overflow = originalStyle.overflow;
      document.body.style.position = originalStyle.position;
      document.body.style.top = originalStyle.top;
      document.body.style.width = originalStyle.width;
      window.scrollTo(0, scrollY);
    };
  }, [isOpen]);

  if (!isOpen) return null;

  return (
    <div
      dir="rtl"
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex flex-col bg-[#FAF7F2] text-[#141D1A]"
      style={{ height: '100dvh', width: '100vw', fontFamily: "'Cairo', 'Tajawal', sans-serif" }}
    >
      <header className="flex items-center justify-between px-5 py-4 bg-[#0F2922] text-white shrink-0 shadow-md">
        <h2 className="text-lg font-bold truncate">{title}</h2>
        <button
          type="button"
          onClick={onClose}
          className="px-4 py-2 rounded-xl bg-white/15 hover:bg-white/25 text-sm font-bold"
        >
          إغلاق ✕
        </button>
      </header>
      <main className="flex-1 overflow-y-auto overscroll-contain p-5">
        {children}
      </main>
    </div>
  );
};
