interface Props { title: string; sub: string; }

export function PlaceholderPage({ title, sub }: Props) {
  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">{title}</h1>
        <p className="page-subtitle">{sub}</p>
      </div>
      <div className="card" style={{ textAlign: 'center', padding: '48px 24px' }}>
        <p style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-sm)' }}>
          준비 중입니다
        </p>
      </div>
    </div>
  );
}
