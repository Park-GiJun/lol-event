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
      <ol className="breadcrumb-list">
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
              <li aria-hidden="true" className="breadcrumb-separator">
                /
              </li>
            )}
          </Fragment>
        ))}
      </ol>
    </nav>
  );
});
