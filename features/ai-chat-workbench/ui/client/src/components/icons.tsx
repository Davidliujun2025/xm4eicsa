export type IconProps = { className?: string };

export const Bot = (p: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" className={p.className}>
    <rect x="3" y="7" width="18" height="12" rx="4" fill="currentColor" opacity=".15" />
    <rect x="3" y="7" width="18" height="12" rx="4" stroke="currentColor" strokeWidth="1.7" />
    <circle cx="9" cy="13" r="1.4" fill="currentColor" />
    <circle cx="15" cy="13" r="1.4" fill="currentColor" />
    <path d="M12 3v4M2 12v3M22 12v3" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
  </svg>
);

export const Chat = (p: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" className={p.className}>
    <path d="M4 5h16v11H8l-4 3V5Z" stroke="currentColor" strokeWidth="1.7" strokeLinejoin="round" />
  </svg>
);

export const Clock = (p: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" className={p.className}>
    <circle cx="12" cy="12" r="8.5" stroke="currentColor" strokeWidth="1.7" />
    <path d="M12 7.5V12l3 2" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
  </svg>
);

export const Book = (p: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" className={p.className}>
    <path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H12v16H6.5A2.5 2.5 0 0 0 4 21.5V5.5ZM20 5.5A2.5 2.5 0 0 0 17.5 3H12v16h5.5a2.5 2.5 0 0 1 2.5 2.5V5.5Z" stroke="currentColor" strokeWidth="1.6" strokeLinejoin="round" />
  </svg>
);

export const Clip = (p: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" className={p.className}>
    <rect x="5" y="4" width="14" height="17" rx="2.5" stroke="currentColor" strokeWidth="1.7" />
    <path d="M9 4h6v2.5H9zM9 12l2 2 4-4" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

export const Chart = (p: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" className={p.className}>
    <path d="M4 20V4M4 20h16M8 16v-4M12 16V8M16 16v-6" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
  </svg>
);

export const Help = (p: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" className={p.className}>
    <circle cx="12" cy="12" r="8.5" stroke="currentColor" strokeWidth="1.6" />
    <path d="M9.5 9.5a2.5 2.5 0 1 1 3.5 2.3c-.8.4-1 .9-1 1.7M12 16.5h.01" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
  </svg>
);

export const Bell = (p: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" className={p.className}>
    <path d="M6 9a6 6 0 0 1 12 0c0 5 2 6 2 6H4s2-1 2-6ZM10 19a2 2 0 0 0 4 0" stroke="currentColor" strokeWidth="1.6" strokeLinejoin="round" />
  </svg>
);

export const Chev = (p: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" className={p.className}>
    <path d="m6 9 6 6 6-6" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

export const Search = (p: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" className={p.className}>
    <circle cx="11" cy="11" r="6.5" stroke="currentColor" strokeWidth="1.7" />
    <path d="m20 20-3.5-3.5" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
  </svg>
);

export const Plus = (p: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" className={p.className}>
    <path d="M12 5v14M5 12h14" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" />
  </svg>
);

export const Check = (p: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" className={p.className}>
    <path d="m5 12.5 4.5 4.5L19 7" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

export const Spark = (p: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" className={p.className}>
    <path d="M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8L12 3Z" fill="currentColor" />
    <path d="M19 14l.7 2 2 .7-2 .7-.7 2-.7-2-2-.7 2-.7.7-2Z" fill="currentColor" />
  </svg>
);

export const Flag = (p: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" className={p.className}>
    <path d="M6 21V4M6 4h11l-2 3.5L17 11H6" stroke="currentColor" strokeWidth="1.6" strokeLinejoin="round" />
  </svg>
);

export const Arrow = (p: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" className={p.className}>
    <path d="M9 6l6 6-6 6" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

export const Tick = (p: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" className={p.className}>
    <circle cx="12" cy="12" r="9" stroke="currentColor" strokeWidth="1.6" />
    <path d="m8.5 12 2.5 2.5 4.5-5" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);