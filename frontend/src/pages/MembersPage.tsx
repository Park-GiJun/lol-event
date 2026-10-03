import { TrashIcon, UserPlusIcon, UsersIcon } from '@/components/icons/LolIcons';
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
	deleteMember,
	getMembers,
	registerMember,
	registerMembers,
	type Member
} from '@/api/member/memberApi';
import { PlayerLink } from '../components/common/PlayerLink';
import { Button } from '../components/common/Button';
import { Input, Textarea } from '../components/common/Input';
import { Modal } from '../components/common/Modal';
import { LoadingCenter } from '../components/common/Spinner';

const MEMBERS_QUERY_KEY = ['members'] as const;
const NO_MEMBERS: Member[] = [];

export function MembersPage() {
	const [showRegister, setShowRegister] = useState(false);
	const [showBulk, setShowBulk] = useState(false);
	const [riotId, setRiotId] = useState('');
	const [bulkInput, setBulkInput] = useState('');
	const queryClient = useQueryClient();
	const { data: members = NO_MEMBERS, isLoading: loading } = useQuery({
		queryKey: MEMBERS_QUERY_KEY,
		queryFn: ({ signal }) => getMembers({ signal })
	});
	const refresh = () => queryClient.invalidateQueries({ queryKey: MEMBERS_QUERY_KEY });

	const register = useMutation({
		mutationFn: registerMember,
		onSuccess: refresh,
		meta: { successMessage: '멤버를 등록했습니다.' }
	});
	// 건별 결과를 화면이 표로 보여 주므로 성공 알림은 따로 내지 않는다.
	const bulk = useMutation({ mutationFn: registerMembers, onSuccess: refresh });
	const remove = useMutation({
		mutationFn: deleteMember,
		onSuccess: refresh,
		meta: { successMessage: '멤버를 삭제했습니다.' }
	});
	const submitting = register.isPending || bulk.isPending;
	const bulkResult = bulk.data ?? null;

	const handleRegister = () => {
		if (!riotId.includes('#')) return;
		register.mutate(riotId, {
			onSuccess: () => {
				setRiotId('');
				setShowRegister(false);
			}
		});
	};

	const handleBulk = () => {
		bulk.mutate(bulkInput.split('\n').filter((l) => l.trim()));
	};

	const handleDelete = (puuid: string) => {
		if (!confirm('삭제하시겠습니까?')) return;
		remove.mutate(puuid);
	};

	return (
		<div className="t-page">
			<div className="hero-banner page-header flex items-center justify-between">
				<div>
					<div className="hero-eyebrow">Member Roster</div>
					<h1 className="hero-title">멤버 관리</h1>
					<p className="hero-subtitle">등록된 멤버 {members.length}명</p>
				</div>
				<div className="flex gap-sm">
					<Button variant="secondary" size="sm" onClick={() => setShowBulk(true)}>
						<UsersIcon size={14} /> 일괄 등록
					</Button>
					<Button size="sm" onClick={() => setShowRegister(true)}>
						<UserPlusIcon size={14} /> 멤버 등록
					</Button>
				</div>
			</div>

			{loading ? (
				<LoadingCenter />
			) : (
				<div className="card">
					<div className="table-wrapper">
						<table className="table">
							<thead>
								<tr>
									<th>Riot ID</th>
									<th>PUUID</th>
									<th>등록일</th>
									<th></th>
								</tr>
							</thead>
							<tbody>
								{members.map((m) => (
									<tr key={m.puuid}>
										<td className="font-semibold">
											<PlayerLink riotId={m.riotId}>{m.riotId}</PlayerLink>
										</td>
										<td className="text-secondary text-xs truncate" style={{ maxWidth: '200px' }}>
											{m.puuid}
										</td>
										<td className="text-secondary">
											{new Date(m.registeredAt).toLocaleDateString('ko-KR')}
										</td>
										<td>
											<Button variant="ghost" size="sm" onClick={() => handleDelete(m.puuid)}>
												<TrashIcon size={14} color="var(--color-error)" />
											</Button>
										</td>
									</tr>
								))}
								{!members.length && (
									<tr>
										<td
											colSpan={4}
											className="text-secondary"
											style={{ textAlign: 'center', padding: 'var(--spacing-xl)' }}
										>
											등록된 멤버 없음
										</td>
									</tr>
								)}
							</tbody>
						</table>
					</div>
				</div>
			)}

			<Modal
				isOpen={showRegister}
				onClose={() => setShowRegister(false)}
				title="멤버 등록"
				size="sm"
				footer={
					<>
						<Button variant="secondary" onClick={() => setShowRegister(false)}>
							취소
						</Button>
						<Button loading={submitting} onClick={handleRegister}>
							등록
						</Button>
					</>
				}
			>
				<Input label="Riot ID" value={riotId} onChange={setRiotId} placeholder="게임명#KR1" />
			</Modal>

			<Modal
				isOpen={showBulk}
				onClose={() => {
					setShowBulk(false);
					bulk.reset();
				}}
				title="일괄 등록"
				size="md"
				footer={
					!bulkResult ? (
						<>
							<Button variant="secondary" onClick={() => setShowBulk(false)}>
								취소
							</Button>
							<Button loading={submitting} onClick={handleBulk}>
								등록
							</Button>
						</>
					) : (
						<Button
							onClick={() => {
								setShowBulk(false);
								bulk.reset();
							}}
						>
							닫기
						</Button>
					)
				}
			>
				{!bulkResult ? (
					<Textarea
						label="Riot ID 목록 (줄 구분)"
						value={bulkInput}
						onChange={setBulkInput}
						placeholder={'플레이어1#KR1\n플레이어2#KR2'}
						rows={8}
					/>
				) : (
					<div>
						{bulkResult.results.map((r, i) => (
							<div key={i} className="pill-row">
								<span className="pill-row-name">{r.riotId}</span>
								<span
									className={`badge pill-row-value ${r.status === 'ok' ? 'badge-win' : r.status === 'error' ? 'badge-loss' : 'badge-normal'}`}
								>
									{r.status === 'ok'
										? '등록'
										: r.status === 'skip'
											? `스킵: ${r.reason}`
											: `오류: ${r.reason}`}
								</span>
							</div>
						))}
					</div>
				)}
			</Modal>
		</div>
	);
}
