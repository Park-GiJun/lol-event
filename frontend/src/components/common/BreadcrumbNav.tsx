import { memo, Fragment } from 'react';
import { useNavigate } from 'react-router-dom';

interface BreadcrumbNavItem {
  label: string;
  path?: string; // undefined → 현재 페이지 (클릭 불가)
}

export const BreadcrumbNav = memo(function BreadcrumbNav({ items }: { items: BreadcrumbNavItem[] }) {
  const navigate = useNavigate();
  return (
    <nav className="breadcrumb-nav" aria-label="breadcrumb">
      <ol style={{ listStyle: 'none', margin: 0, padding: 0, display: 'flex', alignItems: 'center', gap: 'var(--spacing-xs)', flexWrap: 'wrap' }}>
        {items.map((item, i) => (
          <Fragment key={item.path ?? `current-${item.label}`}>
            <li>
              {item.path ? (
                <button className="breadcrumb-btn" onClick={() => navigate(item.path!)}>
                  {item.label}
                </button>
              ) : (
                <span aria-current="page">{item.label}</span>
              )}
            </li>
            {i < items.length - 1 && (
              <li aria-hidden="true" style={{ color: 'var(--color-text-disabled)', fontSize: 'var(--font-size-xs)', lineHeight: 1 }}>
                /
              </li>
            )}
          </Fragment>
        ))}
      </ol>
    </nav>
  );
});
