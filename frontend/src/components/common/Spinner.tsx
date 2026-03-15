export function Spinner({ size = 'md' }: { size?: 'sm' | 'md' | 'lg' }) {
  return <span className={`spinner spinner-${size}`} />;
}

export function LoadingCenter() {
  return <div className="loading-center"><Spinner size="lg" /></div>;
}
