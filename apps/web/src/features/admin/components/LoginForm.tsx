"use client";

import { useId, useState, type FormEvent } from "react";

import { Button } from "@/shared/components/ui/button";
import { Input } from "@/shared/components/ui/input";
import { Label } from "@/shared/components/ui/label";

import { useLogin } from "../api/auth";
import type { Admin } from "../api/types";
import { describeError } from "../model/errorMessage";
import { Notice } from "./Notice";
import { useSubmitLock } from "./useSubmitLock";

type LoginFormProps = {
  onSuccess: (admin: Admin) => void;
  submitLabel?: string;
};

/** 관리자 로그인 폼, 재인증 다이얼로그에서도 재사용, 비밀번호는 컴포넌트 메모리에만 보관 */
export function LoginForm({ onSuccess, submitLabel = "로그인" }: LoginFormProps) {
  const id = useId();
  const login = useLogin();
  const lock = useSubmitLock();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);

  const submit = (event: FormEvent) => {
    event.preventDefault();
    void lock(async () => {
      setError(null);
      try {
        const admin = await login.mutateAsync({ email, password });
        onSuccess(admin);
      } catch (caught) {
        setPassword("");
        setError(describeError(caught, "login").message);
      }
    });
  };

  return (
    <form onSubmit={submit} className="grid gap-6" noValidate>
      {error ? <Notice tone="alert">{error}</Notice> : null}
      <div className="grid gap-2">
        <Label htmlFor={`${id}-email`}>이메일</Label>
        <Input
          id={`${id}-email`}
          type="email"
          spellCheck={false}
          autoComplete="username"
          required
          value={email}
          onChange={(event) => setEmail(event.target.value)}
        />
      </div>
      <div className="grid gap-2">
        <Label htmlFor={`${id}-password`}>비밀번호</Label>
        <Input
          id={`${id}-password`}
          type="password"
          autoComplete="current-password"
          required
          value={password}
          onChange={(event) => setPassword(event.target.value)}
        />
      </div>
      <Button type="submit" disabled={login.isPending} className="w-full">
        {login.isPending ? "로그인 중…" : submitLabel}
      </Button>
    </form>
  );
}
